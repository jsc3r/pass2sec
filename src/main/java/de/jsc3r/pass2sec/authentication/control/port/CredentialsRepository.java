package de.jsc3r.pass2sec.authentication.control.port;

import java.net.URI;
import java.util.Optional;

public interface CredentialsRepository {
    Optional<String> findApiKey(URI server);

    void saveApiKey(URI server, String apiKey);
}
