package de.jsc3r.pass2sec.authentication.control.port;

import java.util.function.Supplier;

public interface SimpleLoginIdentityProvider {
    String login(String email, char[] password, String device, Supplier<char[]> otp);

    void validateApiKey(String apiKey);
}
