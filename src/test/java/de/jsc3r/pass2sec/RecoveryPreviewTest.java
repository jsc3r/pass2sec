package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.control.RecoveryService;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.migration.entity.VaultUpdateState;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.vault.entity.Account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class RecoveryPreviewTest {
    @TempDir Path temp;
    final ObjectMapper json = new ObjectMapper();
    final String key = Base64.getEncoder().encodeToString(new byte[32]);
    final List<String> events = new ArrayList<>();
    final FakePass pass = new FakePass(events);
    final FakeWebsite website = new FakeWebsite(events);

    @Test
    void previewOnlyReadsVaultAndNeverVerifiesOrCommits() throws Exception {
        Rotation rotation = rotation(Stage.WEBSITE_STARTED);
        save(rotation);
        Path file = temp.resolve("state").resolve(EncryptedFileStore.fileName("vault\0item"));
        byte[] before = Files.readAllBytes(file);
        website.failVerify = true;
        pass.failUpdate = true;
        try (var store = readOnly()) {
            var journal = new Journal(store);
            var plan =
                    new RecoveryService(pass, website, journal)
                            .preview("vault", "item", RotationServiceTest.profile(), true);
            assertEquals(VaultUpdateState.ORIGINAL, plan.vaultState());
            assertEquals(List.of("read"), events);
            assertEquals(rotation, journal.read("vault", "item").orElseThrow());
        }
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void previewRecognizesAlreadyUpdatedVaultWithoutWriting() {
        Rotation rotation = rotation(Stage.WEBSITE_VERIFIED);
        save(rotation);
        pass.current =
                new Account(
                        "vault",
                        "item",
                        "Example",
                        "user",
                        rotation.email(),
                        rotation.password(),
                        rotation.original().urls());
        try (var store = readOnly()) {
            var plan =
                    new RecoveryService(pass, website, new Journal(store))
                            .preview("vault", "item", RotationServiceTest.profile(), true);
            assertEquals(VaultUpdateState.UPDATED, plan.vaultState());
            assertEquals(List.of("read"), events);
        }
    }

    @Test
    void previewDetectsConcurrentVaultChangesWithoutOverwriting() {
        save(rotation(Stage.WEBSITE_STARTED));
        pass.current =
                new Account(
                        "vault",
                        "item",
                        "Example",
                        "user",
                        "third@example.org",
                        "synthetic-third-secret",
                        RotationServiceTest.OLD.urls());
        try (var store = readOnly()) {
            var plan =
                    new RecoveryService(pass, website, new Journal(store))
                            .preview("vault", "item", RotationServiceTest.profile(), true);
            assertEquals(VaultUpdateState.CONFLICT, plan.vaultState());
            assertEquals(List.of("read"), events);
        }
    }

    @Test
    void blockedOrCommittedStagesDoNotAccessWebsiteOrVault() {
        for (Stage stage : List.of(Stage.PREPARED, Stage.ALIAS_READY, Stage.COMMITTED)) {
            save(rotation(stage));
            events.clear();
            try (var store = readOnly()) {
                var plan =
                        new RecoveryService(pass, website, new Journal(store))
                                .preview("vault", "item", RotationServiceTest.profile(), true);
                assertEquals(stage, plan.stage());
                assertEquals(VaultUpdateState.NOT_CHECKED, plan.vaultState());
                assertTrue(events.isEmpty());
            }
        }
    }

    private Rotation rotation(Stage stage) {
        return new Rotation(
                RotationServiceTest.OLD,
                "example",
                "alias@example.org",
                "synthetic-new-secret",
                false,
                stage);
    }

    private void save(Rotation rotation) {
        try (var store = new EncryptedFileStore(temp.resolve("state"), key, json)) {
            new Journal(store).save(rotation);
        }
    }

    private EncryptedFileStore readOnly() {
        return EncryptedFileStore.readOnly(temp.resolve("state"), key, json);
    }
}
