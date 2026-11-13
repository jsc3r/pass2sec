package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.boundary.TemplateValues;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.profile.boundary.ProfileLoader;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.boundary.PassItemDecoder;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class ProfileAndPassTest {
    final ObjectMapper json = new ObjectMapper();

    @Test
    void parsesCurrentOfficialPassEnvelopeAndOlderFfiEnvelope() throws Exception {
        var current =
                json.readTree(
                        """
                        {"item":{"id":"item","share_id":"vault","content":{"title":"Example",
                          "content":{"Login":{"username":"user","email":"old@example.org","password":"old-secret","urls":["https://example.org"]}}}}}
                        """);
        assertEquals(
                RotationServiceTest.OLD, new PassItemDecoder().decode("vault", "item", current));
        var legacy =
                json.readTree(
                        """
                        {"item":{"id":"item","share_id":"vault","content":{"title":"Example",
                          "content":{"type":"Login","key":{"username":"user","email":"old@example.org","password":"old-secret","urls":["https://example.org"]}}}}}
                        """);
        assertEquals(
                RotationServiceTest.OLD, new PassItemDecoder().decode("vault", "item", legacy));
        assertThrows(Failure.class, () -> new PassItemDecoder().decode("vault", "other", current));
    }

    @Test
    void rejectsUnknownJsonRatherThanGuessingSecrets() throws Exception {
        var root = json.readTree("{\"password\":\"secret\"}");
        assertThrows(Failure.class, () -> new PassItemDecoder().decode("vault", "item", root));
    }

    @Test
    void rejectsUntrustedOrigins() {
        var profile = RotationServiceTest.profile();
        for (var url :
                new String[] {
                    "http://example.org",
                    "https://evil.example",
                    "https://example.org.evil.example",
                    "https://example.org:444",
                    "https://user@example.org"
                }) assertThrows(Failure.class, () -> profile.checkUrl(url));
        assertDoesNotThrow(() -> profile.checkUrl("https://example.org/settings"));
    }

    @Test
    void validatesBundledExampleAndRequiresInteractiveMode() {
        var profile = new ProfileLoader(json).load(Path.of("profiles/example.json"));
        assertThrows(Failure.class, () -> profile.checkMode(true));
        assertDoesNotThrow(() -> profile.checkMode(false));
    }

    @Test
    void resolvesOldLoginSeparatelyFromNewLogin() {
        var rotation =
                new Rotation(
                        RotationServiceTest.OLD,
                        "example",
                        "new@example.org",
                        "new-secret",
                        true,
                        Stage.WEBSITE_STARTED);
        var profile = RotationServiceTest.profile();
        assertEquals("user", TemplateValues.resolve("${username}", rotation));
        assertEquals("new@example.org", TemplateValues.resolve("${newEmail}", rotation));
        assertFalse(rotation.toString().contains("new-secret"));
        assertFalse(rotation.original().toString().contains("old-secret"));
    }
}
