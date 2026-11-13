package de.jsc3r.pass2sec.shared.boundary;

import static java.nio.file.StandardOpenOption.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.shared.entity.Failure;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** AES-GCM envelope, private directory, atomic replacement and one process per state directory. */
public final class EncryptedFileStore implements AutoCloseable {
    private final Path directory;
    private final byte[] key;
    private final ObjectMapper json;
    private final FileChannel channel;
    private final FileLock lock;
    private final boolean readOnly;

    public EncryptedFileStore(Path directory, String encodedKey, ObjectMapper json) {
        this(directory, encodedKey, json, false);
    }

    public static EncryptedFileStore readOnly(
            Path directory, String encodedKey, ObjectMapper json) {
        return new EncryptedFileStore(directory, encodedKey, json, true);
    }

    private EncryptedFileStore(
            Path directory, String encodedKey, ObjectMapper json, boolean readOnly) {
        this.readOnly = readOnly;
        try {
            this.key = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey);
            if (key.length != 32)
                throw new Failure(
                        "PASS2SEC_JOURNAL_KEY muss 32 zufällige Bytes in Base64 enthalten.");
            this.json = json;
            this.directory = directory.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(this.directory))
                throw new Failure("Journal-Verzeichnis darf kein Symlink sein.");
            if (!readOnly)
                Files.createDirectories(
                        this.directory,
                        PosixFilePermissions.asFileAttribute(
                                PosixFilePermissions.fromString("rwx------")));
            if (!Files.getPosixFilePermissions(this.directory)
                    .equals(PosixFilePermissions.fromString("rwx------")))
                throw new Failure("Journal-Verzeichnis benötigt Rechte 0700.");
            if (readOnly) {
                channel = null;
                lock = null;
                return;
            }
            Path lockFile = this.directory.resolve(".lock");
            channel =
                    FileChannel.open(
                            lockFile,
                            Set.of(CREATE, WRITE, LinkOption.NOFOLLOW_LINKS),
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString("rw-------")));
            FileLock acquired;
            try {
                acquired = channel.tryLock();
            } catch (Exception e) {
                channel.close();
                throw e;
            }
            if (acquired == null) {
                channel.close();
                throw new Failure("Ein anderer pass2sec-Prozess verwendet dieses Journal.");
            }
            lock = acquired;
        } catch (Failure e) {
            throw e;
        } catch (Exception e) {
            throw new Failure(
                    "Journal nicht verfügbar: Schlüssel, Rechte oder Prozesssperre prüfen (POSIX"
                            + " erforderlich).");
        }
    }

    public static String fileName(String identity) {
        try {
            var hash =
                    MessageDigest.getInstance("SHA-256")
                            .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash) + ".enc";
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path path(String fileName) {
        if (!fileName.matches("[a-f0-9]{64}\\.enc"))
            throw new Failure("Ungültiger Speicherdateiname.");
        return directory.resolve(fileName);
    }

    public <T> Optional<T> read(String fileName, Class<T> type) {
        Path file = path(fileName);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        try {
            if (Files.isSymbolicLink(file))
                throw new Failure("Journal-Datei darf kein Symlink sein.");
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length < 29 || bytes[0] != 1) throw new GeneralSecurityException();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, bytes, 1, 12));
            cipher.updateAAD(
                    file.getFileName()
                            .toString()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Optional.of(json.readValue(cipher.doFinal(bytes, 13, bytes.length - 13), type));
        } catch (Exception e) {
            throw new Failure(
                    "Journal kann nicht entschlüsselt werden: Schlüssel falsch oder Datei"
                            + " beschädigt.");
        }
    }

    public void save(String fileName, Object value) {
        if (readOnly)
            throw new Failure(
                    "Schreibzugriff auf einen nur lesend geöffneten Speicher verweigert.");
        Path temp = null;
        try {
            verifyExistingKey();
            Path target = path(fileName);
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, nonce));
            cipher.updateAAD(
                    target.getFileName()
                            .toString()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(json.writeValueAsBytes(value));
            var envelope =
                    ByteBuffer.allocate(13 + ciphertext.length)
                            .put((byte) 1)
                            .put(nonce)
                            .put(ciphertext)
                            .flip();
            temp =
                    Files.createTempFile(
                            directory,
                            ".rotation-",
                            ".tmp",
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString("rw-------")));
            try (var output = FileChannel.open(temp, WRITE)) {
                while (envelope.hasRemaining()) output.write(envelope);
                output.force(true);
            }
            Files.move(
                    temp,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            try (var dir = FileChannel.open(directory, READ)) {
                dir.force(true);
            }
        } catch (Exception e) {
            throw new Failure(
                    "Journal konnte nicht dauerhaft gespeichert werden; Vorgang angehalten.");
        } finally {
            if (temp != null)
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
        }
    }

    /** Prevent storing new credentials under a different key in an existing state directory. */
    public void verifyExistingKey() {
        try (var files = Files.list(directory)) {
            files.filter(file -> file.getFileName().toString().matches("[a-f0-9]{64}\\.enc"))
                    .findFirst()
                    .ifPresent(
                            file ->
                                    read(
                                            file.getFileName().toString(),
                                            com.fasterxml.jackson.databind.JsonNode.class));
        } catch (Failure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new Failure("Verschlüsselter Speicher konnte nicht geprüft werden.");
        }
    }

    @Override
    public void close() {
        Arrays.fill(key, (byte) 0);
        try {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
        } catch (Exception e) {
            throw new Failure("Journal-Sperre konnte nicht geschlossen werden.");
        }
    }
}
