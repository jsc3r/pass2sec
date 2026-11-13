package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.shared.entity.Failure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

class ReadOnlyStateTest {
    @TempDir Path temp;
    final ObjectMapper json = new ObjectMapper();
    final String key = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void readOnlyStoreReadsExistingDataWithoutRecreatingLockOrChangingCiphertext()
            throws Exception {
        Path state = temp.resolve("state");
        var rotation =
                new Rotation(
                        RotationServiceTest.OLD,
                        "example",
                        "alias@example.org",
                        "synthetic-secret",
                        false,
                        Stage.WEBSITE_STARTED);
        try (var store = new EncryptedFileStore(state, key, json)) {
            new Journal(store).save(rotation);
        }
        Files.delete(state.resolve(".lock")); // Synthetic fixture; no writer is running.
        Path ciphertext = state.resolve(EncryptedFileStore.fileName("vault\0item"));
        byte[] before = Files.readAllBytes(ciphertext);
        var modified = Files.getLastModifiedTime(ciphertext);
        try (var store = EncryptedFileStore.readOnly(state, key, json)) {
            assertEquals(rotation, new Journal(store).read("vault", "item").orElseThrow());
            assertThrows(
                    Failure.class, () -> new Journal(store).save(rotation.at(Stage.COMMITTED)));
            assertFalse(Files.exists(state.resolve(".lock")));
        }
        assertArrayEquals(before, Files.readAllBytes(ciphertext));
        assertEquals(modified, Files.getLastModifiedTime(ciphertext));
        assertFalse(Files.exists(state.resolve(".lock")));
    }

    @Test
    void readOnlyOpenNeverCreatesMissingDirectory() {
        Path missing = temp.resolve("missing");
        assertThrows(Failure.class, () -> EncryptedFileStore.readOnly(missing, key, json));
        assertFalse(Files.exists(missing));
    }
}
