package de.jsc3r.pass2sec.application.boundary.cli;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.alias.boundary.SimpleLogin;
import de.jsc3r.pass2sec.alias.control.port.Aliases;
import de.jsc3r.pass2sec.authentication.boundary.ConsoleSecretInput;
import de.jsc3r.pass2sec.authentication.boundary.EncryptedCredentialsRepository;
import de.jsc3r.pass2sec.authentication.boundary.HelpAwareSecretInput;
import de.jsc3r.pass2sec.authentication.boundary.SimpleLoginAuthenticator;
import de.jsc3r.pass2sec.authentication.control.AuthenticationService;
import de.jsc3r.pass2sec.authentication.control.port.SecretInput;
import de.jsc3r.pass2sec.migration.boundary.BrowserWebsite;
import de.jsc3r.pass2sec.migration.control.port.Website;
import de.jsc3r.pass2sec.profile.boundary.ProfileLoader;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.shared.boundary.HttpJsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.boundary.PassCli;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;

/** Composition root: constructs concrete adapters outside the application services. */
public final class ApplicationComponents {
    private final String binary;
    private final Path stateDir;
    private final URI simpleLoginUrl;
    private final ObjectMapper json = new ObjectMapper();
    private final SecretInput input;

    public ApplicationComponents(
            String binary, Path stateDir, URI simpleLoginUrl, Runnable showHelp) {
        this.binary = binary;
        this.stateDir = stateDir;
        this.simpleLoginUrl = simpleLoginUrl;
        this.input = new HelpAwareSecretInput(new ConsoleSecretInput(), showHelp);
    }

    public PassCli pass() {
        return new PassCli(binary, json);
    }

    public ProfileLoader profiles() {
        return new ProfileLoader(json);
    }

    public Website website() {
        return new BrowserWebsite();
    }

    public URI simpleLoginUrl() {
        return simpleLoginUrl;
    }

    public EncryptedFileStore openState() {
        return openState(false);
    }

    public boolean stateExists() {
        return Files.exists(stateDir, LinkOption.NOFOLLOW_LINKS);
    }

    public EncryptedFileStore openReadOnlyState() {
        if (!stateExists())
            throw new Failure("Kein lokaler Zustand vorhanden; --state-dir prüfen.");
        return openState(true);
    }

    private EncryptedFileStore openState(boolean readOnly) {
        String key = System.getenv("PASS2SEC_JOURNAL_KEY");
        if (key == null || key.isBlank()) {
            char[] secret =
                    input.readSecret(
                            "Journal-Schlüssel (Base64; vorher mit keygen erzeugen und sichern): ");
            try {
                key = new String(secret);
            } finally {
                Arrays.fill(secret, '\0');
            }
        }
        var store =
                readOnly
                        ? EncryptedFileStore.readOnly(stateDir, key, json)
                        : new EncryptedFileStore(stateDir, key, json);
        try {
            store.verifyExistingKey();
            return store;
        } catch (RuntimeException failure) {
            store.close();
            throw failure;
        }
    }

    public Aliases aliases(EncryptedFileStore store) {
        String key = System.getenv("SIMPLELOGIN_API_KEY");
        if (key == null || key.isBlank()) {
            key =
                    new EncryptedCredentialsRepository(store)
                            .findApiKey(simpleLoginUrl)
                            .orElseThrow(
                                    () ->
                                            new Failure(
                                                    "SimpleLogin-Zugang fehlt. auth simplelogin"
                                                            + " login oder auth simplelogin set-key"
                                                            + " ausführen."));
        }
        return new SimpleLogin(simpleLoginUrl, key, json);
    }

    public AuthenticationService authentication(EncryptedFileStore store) {
        var provider =
                new SimpleLoginAuthenticator(new HttpJsonTransport(simpleLoginUrl, json), json);
        return new AuthenticationService(
                provider, new EncryptedCredentialsRepository(store), input);
    }

    public String display(String value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new Failure("Ausgabe konnte nicht formatiert werden.");
        }
    }
}
