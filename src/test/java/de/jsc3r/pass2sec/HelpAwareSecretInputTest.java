package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.authentication.boundary.EncryptedCredentialsRepository;
import de.jsc3r.pass2sec.authentication.boundary.HelpAwareSecretInput;
import de.jsc3r.pass2sec.authentication.boundary.SimpleLoginAuthenticator;
import de.jsc3r.pass2sec.authentication.control.AuthenticationService;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

class HelpAwareSecretInputTest {
    @TempDir Path temp;

    @Test
    void lineHelpRepeatsPromptAndLeavesActualInputUnchanged() {
        var input = new FakeSecretInput();
        var helpCount = new AtomicInteger();
        input.lines.add(" HELP ");
        input.lines.add("help");
        input.lines.add("user@example.org");
        var decorated = new HelpAwareSecretInput(input, helpCount::incrementAndGet);
        assertEquals("user@example.org", decorated.readLine("E-Mail: "));
        assertEquals(2, helpCount.get());
    }

    @Test
    void secretHelpIsClearedAndNeverReturnedAsCredential() {
        var input = new FakeSecretInput();
        char[] help = "HeLP".toCharArray();
        char[] credential = "synthetic-secret".toCharArray();
        input.secrets.add(help);
        input.secrets.add(credential);
        var helpCount = new AtomicInteger();
        var decorated = new HelpAwareSecretInput(input, helpCount::incrementAndGet);
        assertSame(credential, decorated.readSecret("Key: "));
        assertArrayEquals(new char[4], help);
        assertEquals(1, helpCount.get());
    }

    @Test
    void escapedHelpCanBeUsedAsLiteralInput() {
        var input = new FakeSecretInput();
        input.lines.add("\\help");
        char[] escaped = "\\HeLP".toCharArray();
        input.secrets.add(escaped);
        var decorated = new HelpAwareSecretInput(input, () -> fail("Literal must not open help"));
        assertEquals("help", decorated.readLine("Text: "));
        assertArrayEquals("HeLP".toCharArray(), decorated.readSecret("Passwort: "));
        assertArrayEquals(new char[5], escaped);
    }

    @Test
    void otherSecretCharactersAreNotTrimmedOrChanged() {
        var input = new FakeSecretInput();
        char[] credential = " help ".toCharArray();
        input.secrets.add(credential);
        var decorated = new HelpAwareSecretInput(input, () -> fail("Must remain a credential"));
        assertSame(credential, decorated.readSecret("Passwort: "));
    }

    @Test
    void helpDuringLoginNeverReachesAuthenticationApi() throws Exception {
        var json = new ObjectMapper();
        var input = new FakeSecretInput();
        input.lines.add("help");
        input.lines.add("user@example.org");
        input.secrets.add("help".toCharArray());
        input.secrets.add("synthetic-password".toCharArray());
        var transport = new FakeJsonTransport();
        transport.responses.add(
                json.readTree("{\"mfa_enabled\":false,\"api_key\":\"synthetic-key\"}"));
        transport.responses.add(json.createObjectNode());
        var helpCount = new AtomicInteger();
        try (var store =
                new EncryptedFileStore(
                        temp.resolve("state"),
                        Base64.getEncoder().encodeToString(new byte[32]),
                        json)) {
            new AuthenticationService(
                            new SimpleLoginAuthenticator(transport, json),
                            new EncryptedCredentialsRepository(store),
                            new HelpAwareSecretInput(input, helpCount::incrementAndGet))
                    .login(URI.create("https://app.simplelogin.io"), "test-device");
        }
        assertEquals(2, helpCount.get());
        assertEquals(2, transport.paths.size());
        assertEquals("user@example.org", transport.bodies.getFirst().path("email").asText());
        assertEquals("synthetic-password", transport.bodies.getFirst().path("password").asText());
    }
}
