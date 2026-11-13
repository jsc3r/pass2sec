package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.shared.entity.Failure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

class JournalTest {
    @TempDir Path temp;
    final ObjectMapper json = new ObjectMapper();
    final String key = Base64.getEncoder().encodeToString(new byte[32]);
    final Rotation rotation =
            new Rotation(
                    RotationServiceTest.OLD,
                    "example",
                    "alias@example.org",
                    "new-secret",
                    false,
                    Stage.WEBSITE_STARTED);

    @Test
    void roundTripsEncryptsAndRestrictsPermissions() throws Exception {
        var dir = temp.resolve("state");
        try (var j = new Journal(dir, key, json)) {
            j.save(rotation);
        }
        Path file;
        try (var files = Files.list(dir)) {
            file = files.filter(p -> p.toString().endsWith(".enc")).findFirst().orElseThrow();
        }
        var encoded =
                new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(encoded.contains("old-secret"));
        assertFalse(encoded.contains("new-secret"));
        assertEquals(
                PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        assertEquals(
                PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(dir));
        try (var j = new Journal(dir, key, json)) {
            assertEquals(rotation, j.read("vault", "item").orElseThrow());
        }
    }

    @Test
    void wrongKeyAndTamperingFailClosed() throws Exception {
        var dir = temp.resolve("state");
        try (var j = new Journal(dir, key, json)) {
            j.save(rotation);
        }
        var wrong = new byte[32];
        Arrays.fill(wrong, (byte) 7);
        try (var j = new Journal(dir, Base64.getEncoder().encodeToString(wrong), json)) {
            assertThrows(Failure.class, () -> j.read("vault", "item"));
        }
        Path file;
        try (var files = Files.list(dir)) {
            file = files.filter(p -> p.toString().endsWith(".enc")).findFirst().orElseThrow();
        }
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 1;
        Files.write(file, bytes);
        try (var j = new Journal(dir, key, json)) {
            assertThrows(Failure.class, () -> j.read("vault", "item"));
        }
    }

    @Test
    void locksOutParallelProcesses() {
        try (var ignored = new Journal(temp.resolve("state"), key, json)) {
            assertThrows(Failure.class, () -> new Journal(temp.resolve("state"), key, json));
        }
    }

    @Test
    void rejectsMissingKey() {
        assertThrows(Failure.class, () -> new Journal(temp.resolve("state"), null, json));
    }
}
