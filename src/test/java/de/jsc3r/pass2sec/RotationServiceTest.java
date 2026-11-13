package de.jsc3r.pass2sec;

import static de.jsc3r.pass2sec.profile.entity.Action.*;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.control.RecoveryService;
import de.jsc3r.pass2sec.migration.control.RotationService;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.profile.entity.Step;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.entity.Account;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

class RotationServiceTest {
    @TempDir Path temp;
    static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    static final Account OLD =
            new Account(
                    "vault",
                    "item",
                    "Example",
                    "user",
                    "old@example.org",
                    "old-secret",
                    List.of("https://example.org"));
    final List<String> events = new ArrayList<>();
    final FakePass pass = new FakePass(events);
    final FakeAliases aliases = new FakeAliases(events);
    final FakeWebsite website = new FakeWebsite(events);
    final Profile profile = profile();
    Journal journal;
    RotationService service;

    static Profile profile() {
        return new Profile(
                "example",
                Set.of("example.org"),
                "example",
                false,
                24,
                30,
                List.of(new Step(NAVIGATE, "https://example.org/settings", null)),
                List.of(
                        new Step(NAVIGATE, "https://example.org/login", null),
                        new Step(FILL, "#email", "${newEmail}"),
                        new Step(FILL, "#password", "${newPassword}"),
                        new Step(CLICK, "#submit", null),
                        new Step(ASSERT_TEXT, "#email", "${newEmail}")));
    }

    @BeforeEach
    void setup() {
        journal = new Journal(temp.resolve("state"), KEY, new ObjectMapper());
        service = new RotationService(pass, aliases, website, journal);
    }

    @AfterEach
    void close() {
        journal.close();
    }

    void rotate() {
        service.rotate(OLD, profile, "@alias.example", List.of(1L), true);
    }

    @Test
    void verifiesBeforeUpdatingAndReadsBack() {
        rotate();
        assertEquals(
                List.of("plan", "generate", "alias", "change", "verify", "read", "update", "read"),
                events);
        assertEquals(Stage.COMMITTED, journal.read("vault", "item").orElseThrow().stage());
        assertEquals("user", pass.current.username());
    }

    @Test
    void verificationFailureNeverUpdatesVaultAndBlocksAnotherRotation() {
        website.failVerify = true;
        assertThrows(Failure.class, this::rotate);
        assertEquals(OLD, pass.current);
        assertEquals(Stage.WEBSITE_STARTED, journal.read("vault", "item").orElseThrow().stage());
        events.clear();
        assertThrows(Failure.class, this::rotate);
        assertTrue(events.isEmpty());
    }

    @Test
    void recoveryOnlyVerifiesAndCommitsWithoutRepeatingWebsiteOrAlias() {
        website.failVerify = true;
        assertThrows(Failure.class, this::rotate);
        website.failVerify = false;
        events.clear();
        new RecoveryService(pass, website, journal).recover("vault", "item", profile, true);
        assertEquals(List.of("verify", "read", "update", "read"), events);
    }

    @Test
    void failedVaultWriteRemainsRecoverable() {
        pass.failUpdate = true;
        assertThrows(Failure.class, this::rotate);
        assertEquals(Stage.WEBSITE_VERIFIED, journal.read("vault", "item").orElseThrow().stage());
        pass.failUpdate = false;
        events.clear();
        new RecoveryService(pass, website, journal).recover("vault", "item", profile, true);
        assertEquals(List.of("verify", "read", "update", "read"), events);
    }

    @Test
    void detectsConcurrentVaultEdit() {
        website.onVerify =
                () ->
                        pass.current =
                                new Account(
                                        "vault",
                                        "item",
                                        "Example",
                                        "user",
                                        "third@example.org",
                                        "another-secret",
                                        OLD.urls());
        assertThrows(Failure.class, this::rotate);
        assertFalse(events.contains("update"));
        assertEquals("third@example.org", pass.current.email());
    }

    @Test
    void aliasFailureIsJournaledAndNotRetried() {
        aliases.fail = true;
        assertThrows(Failure.class, this::rotate);
        assertEquals(Stage.PREPARED, journal.read("vault", "item").orElseThrow().stage());
        assertFalse(events.contains("change"));
        events.clear();
        assertThrows(Failure.class, this::rotate);
        assertTrue(events.isEmpty());
    }

    @Test
    void committedOperationIsNotRepeated() {
        rotate();
        events.clear();
        rotate();
        assertTrue(events.isEmpty());
    }

    @Test
    void vaultWriteSucceededButAcknowledgementLostIsReconciled() {
        pass.loseAcknowledgement = true;
        assertThrows(Failure.class, this::rotate);
        pass.loseAcknowledgement = false;
        events.clear();
        new RecoveryService(pass, website, journal).recover("vault", "item", profile, true);
        assertEquals(List.of("verify", "read"), events);
        assertEquals(Stage.COMMITTED, journal.read("vault", "item").orElseThrow().stage());
    }

    @Test
    void websitePartialFailurePreservesBothSecretsWithoutVaultWrite() {
        website.failChange = true;
        assertThrows(Failure.class, this::rotate);
        var saved = journal.read("vault", "item").orElseThrow();
        assertEquals("old-secret", saved.original().password());
        assertEquals("new-secret", saved.password());
        assertFalse(events.contains("verify"));
        assertFalse(events.contains("update"));
    }

    @Test
    void wrongWebsiteFailsBeforeAnyExternalMutation() {
        var other =
                new Account(
                        "vault", "item", "Other", "u", "e", "p", List.of("https://evil.example"));
        assertThrows(
                Failure.class,
                () -> service.rotate(other, profile, "@alias.example", List.of(1L), true));
        assertTrue(events.isEmpty());
    }
}
