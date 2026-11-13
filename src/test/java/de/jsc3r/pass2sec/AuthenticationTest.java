package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.authentication.boundary.EncryptedCredentialsRepository;
import de.jsc3r.pass2sec.authentication.boundary.SimpleLoginAuthenticator;
import de.jsc3r.pass2sec.authentication.control.AuthenticationService;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.shared.boundary.HttpFailure;
import de.jsc3r.pass2sec.shared.boundary.HttpJsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

class AuthenticationTest {
    @TempDir Path temp;
    final ObjectMapper json = new ObjectMapper();
    final FakeJsonTransport http = new FakeJsonTransport();
    final FakeSecretInput input = new FakeSecretInput();
    final URI server = URI.create("https://app.simplelogin.io");
    final String key = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void passwordLoginValidatesAndPersistsOnlyApiKey() throws Exception {
        char[] password = "fake-password".toCharArray();
        input.lines.add("user@example.org");
        input.secrets.add(password);
        http.responses.add(json.readTree("{\"mfa_enabled\":false,\"api_key\":\"fake-api-key\"}"));
        http.responses.add(json.readTree("{\"email\":\"user@example.org\"}"));
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            service(credentials).login(server, "pass2sec-test");
            assertEquals("fake-api-key", credentials.findApiKey(server).orElseThrow());
            assertEquals(List.of("/api/auth/login", "/api/user_info"), http.paths);
            assertNull(http.apiKeys.getFirst());
            assertEquals("fake-api-key", http.apiKeys.get(1));
            assertEquals("pass2sec-test", http.bodies.getFirst().path("device").asText());
            assertArrayEquals(new char[password.length], password);
        }
        try (var store = store()) {
            assertEquals(
                    "fake-api-key",
                    new EncryptedCredentialsRepository(store).findApiKey(server).orElseThrow());
        }
        try (var files = Files.list(temp.resolve("state"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".enc")).toList()) {
                String data =
                        new String(
                                Files.readAllBytes(file),
                                java.nio.charset.StandardCharsets.ISO_8859_1);
                assertFalse(data.contains("fake-password"));
                assertFalse(data.contains("fake-api-key"));
            }
        }
    }

    @Test
    void completesTotpChallengeBeforeSavingAndClearsInputs() throws Exception {
        char[] password = "password".toCharArray();
        char[] otp = "123456".toCharArray();
        input.lines.add("user@example.org");
        input.secrets.add(password);
        input.secrets.add(otp);
        http.responses.add(
                json.readTree("{\"mfa_enabled\":true,\"mfa_key\":\"challenge\",\"api_key\":\"\"}"));
        http.responses.add(json.readTree("{\"api_key\":\"mfa-api-key\"}"));
        http.responses.add(json.createObjectNode());
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            service(credentials).login(server, "test-device");
            assertEquals(List.of("/api/auth/login", "/api/auth/mfa", "/api/user_info"), http.paths);
            assertEquals("challenge", http.bodies.get(1).path("mfa_key").asText());
            assertEquals("123456", http.bodies.get(1).path("mfa_token").asText());
            assertEquals("mfa-api-key", credentials.findApiKey(server).orElseThrow());
            assertArrayEquals(new char[password.length], password);
            assertArrayEquals(new char[otp.length], otp);
        }
    }

    @Test
    void invalidImportedKeyDoesNotOverwriteWorkingCredentials() {
        input.secrets.add("bad-key".toCharArray());
        http.responses.add(new HttpFailure(401));
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            credentials.saveApiKey(server, "existing-key");
            assertThrows(Failure.class, () -> service(credentials).configureApiKey(server));
            assertEquals("existing-key", credentials.findApiKey(server).orElseThrow());
        }
    }

    @Test
    void failedTotpKeepsPreviousCredentials() throws Exception {
        input.lines.add("user@example.org");
        input.secrets.add("password".toCharArray());
        input.secrets.add("000000".toCharArray());
        http.responses.add(json.readTree("{\"mfa_enabled\":true,\"mfa_key\":\"challenge\"}"));
        http.responses.add(new HttpFailure(401));
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            credentials.saveApiKey(server, "existing-key");
            assertThrows(Failure.class, () -> service(credentials).login(server, "device"));
            assertEquals("existing-key", credentials.findApiKey(server).orElseThrow());
        }
    }

    @Test
    void fidoFailureExplainsApiKeyAlternativeWithoutStoringPassword() {
        input.lines.add("user@example.org");
        char[] password = "private-password".toCharArray();
        input.secrets.add(password);
        http.responses.add(new HttpFailure(403));
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            var failure =
                    assertThrows(Failure.class, () -> service(credentials).login(server, "device"));
            assertTrue(failure.getMessage().contains("set-key"));
            assertFalse(failure.getMessage().contains("private-password"));
            assertTrue(credentials.findApiKey(server).isEmpty());
            assertArrayEquals(new char[password.length], password);
        }
    }

    @Test
    void credentialsAreBoundToOriginAndTrailingSlashIsCanonical() {
        try (var store = store()) {
            var credentials = new EncryptedCredentialsRepository(store);
            credentials.saveApiKey(server, "key");
            assertEquals(
                    "key",
                    credentials
                            .findApiKey(URI.create("https://app.simplelogin.io/"))
                            .orElseThrow());
            assertTrue(credentials.findApiKey(URI.create("https://other.example")).isEmpty());
        }
    }

    @Test
    void wrongEncryptionKeyCannotAddCredentialsToExistingStateDirectory() {
        try (var store = store()) {
            new EncryptedCredentialsRepository(store).saveApiKey(server, "existing-key");
        }
        byte[] wrong = new byte[32];
        wrong[0] = 1;
        try (var store =
                new EncryptedFileStore(
                        temp.resolve("state"), Base64.getEncoder().encodeToString(wrong), json)) {
            assertThrows(Failure.class, store::verifyExistingKey);
            assertThrows(
                    Failure.class,
                    () ->
                            new EncryptedCredentialsRepository(store)
                                    .saveApiKey(URI.create("https://other.example"), "new-key"));
        }
    }

    @Test
    void rejectsUnsafeServerAndCrossOriginRequestBeforeSendingCredentials() {
        assertThrows(
                Failure.class, () -> new HttpJsonTransport(URI.create("http://example.org"), json));
        var transport = new HttpJsonTransport(server, json);
        assertThrows(
                Failure.class,
                () ->
                        transport.request(
                                "POST",
                                "https://other.example/login",
                                null,
                                json.createObjectNode().put("password", "secret"),
                                200));
    }

    private EncryptedFileStore store() {
        return new EncryptedFileStore(temp.resolve("state"), key, json);
    }

    private AuthenticationService service(EncryptedCredentialsRepository credentials) {
        return new AuthenticationService(
                new SimpleLoginAuthenticator(http, json), credentials, input);
    }
}
