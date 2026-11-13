package de.jsc3r.pass2sec.authentication.boundary;

import static de.jsc3r.pass2sec.shared.boundary.JsonFields.required;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.authentication.control.port.SimpleLoginIdentityProvider;
import de.jsc3r.pass2sec.shared.boundary.HttpFailure;
import de.jsc3r.pass2sec.shared.control.port.JsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.util.Arrays;
import java.util.function.Supplier;

public final class SimpleLoginAuthenticator implements SimpleLoginIdentityProvider {
    private final JsonTransport transport;
    private final ObjectMapper json;

    public SimpleLoginAuthenticator(JsonTransport transport, ObjectMapper json) {
        this.transport = transport;
        this.json = json;
    }

    @Override
    public String login(String email, char[] password, String device, Supplier<char[]> otp) {
        var body =
                json.createObjectNode()
                        .put("email", email)
                        .put("password", new String(password))
                        .put("device", device);
        try {
            var result = transport.request("POST", "/api/auth/login", null, body, 200);
            if (!result.path("mfa_enabled").isBoolean())
                throw new Failure("SimpleLogin lieferte keinen eindeutigen MFA-Status.");
            if (!result.path("mfa_enabled").asBoolean()) return required(result, "api_key");
            String mfaKey = required(result, "mfa_key");
            char[] token = otp.get();
            var challenge = json.createObjectNode();
            try {
                challenge
                        .put("mfa_key", mfaKey)
                        .put("mfa_token", new String(token))
                        .put("device", device);
                return required(
                        transport.request("POST", "/api/auth/mfa", null, challenge, 200),
                        "api_key");
            } finally {
                challenge.removeAll();
                Arrays.fill(token, '\0');
            }
        } catch (HttpFailure failure) {
            if (failure.status() == 403) {
                throw new Failure(
                        "SimpleLogin verweigert den Passwort-Login (z. B. FIDO). Vorhandenen"
                                + " API-Key mit auth simplelogin set-key einrichten.");
            }
            throw failure;
        } finally {
            body.remove("password");
        }
    }

    @Override
    public void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new Failure("Leerer SimpleLogin-API-Key.");
        transport.request("GET", "/api/user_info", apiKey, null, 200);
    }
}
