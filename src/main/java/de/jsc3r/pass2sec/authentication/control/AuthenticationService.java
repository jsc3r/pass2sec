package de.jsc3r.pass2sec.authentication.control;

import de.jsc3r.pass2sec.authentication.control.port.CredentialsRepository;
import de.jsc3r.pass2sec.authentication.control.port.SecretInput;
import de.jsc3r.pass2sec.authentication.control.port.SimpleLoginIdentityProvider;

import java.net.URI;
import java.util.Arrays;

public final class AuthenticationService {
    private final SimpleLoginIdentityProvider provider;
    private final CredentialsRepository credentials;
    private final SecretInput input;

    public AuthenticationService(
            SimpleLoginIdentityProvider provider,
            CredentialsRepository credentials,
            SecretInput input) {
        this.provider = provider;
        this.credentials = credentials;
        this.input = input;
    }

    public void login(URI server, String device) {
        credentials.findApiKey(
                server); // Check existing encrypted credentials before creating another remote key.
        String email = input.readLine("SimpleLogin E-Mail: ");
        char[] password = input.readSecret("SimpleLogin Passwort: ");
        try {
            String apiKey =
                    provider.login(
                            email, password, device, () -> input.readSecret("SimpleLogin TOTP: "));
            validateAndSave(server, apiKey);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public void configureApiKey(URI server) {
        credentials.findApiKey(server);
        char[] apiKey = input.readSecret("SimpleLogin API-Key: ");
        try {
            validateAndSave(server, new String(apiKey));
        } finally {
            Arrays.fill(apiKey, '\0');
        }
    }

    private void validateAndSave(URI server, String apiKey) {
        provider.validateApiKey(apiKey);
        credentials.saveApiKey(server, apiKey);
    }
}
