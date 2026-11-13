package de.jsc3r.pass2sec.authentication.boundary;

import de.jsc3r.pass2sec.authentication.control.port.CredentialsRepository;
import de.jsc3r.pass2sec.authentication.entity.ApiCredentials;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.net.URI;
import java.util.Optional;

public final class EncryptedCredentialsRepository implements CredentialsRepository {
    private final EncryptedFileStore store;

    public EncryptedCredentialsRepository(EncryptedFileStore store) {
        this.store = store;
    }

    @Override
    public Optional<String> findApiKey(URI server) {
        String identity = canonicalServer(server);
        return store.read(fileName(identity), ApiCredentials.class)
                .map(
                        credentials -> {
                            if (!identity.equals(credentials.server())
                                    || credentials.apiKey() == null
                                    || credentials.apiKey().isBlank()) {
                                throw new Failure(
                                        "Gespeicherte SimpleLogin-Zugangsdaten passen nicht zum"
                                                + " Server.");
                            }
                            return credentials.apiKey();
                        });
    }

    @Override
    public void saveApiKey(URI server, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new Failure("Leerer SimpleLogin-API-Key.");
        String identity = canonicalServer(server);
        store.save(fileName(identity), new ApiCredentials(identity, apiKey));
    }

    private String canonicalServer(URI server) {
        return server.resolve("/").toString();
    }

    private String fileName(String identity) {
        return EncryptedFileStore.fileName("simplelogin-api-key\0" + identity);
    }
}
