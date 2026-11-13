package de.jsc3r.pass2sec.shared.boundary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.shared.control.port.JsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

public final class HttpJsonTransport implements JsonTransport {
    private final URI base;
    private final ObjectMapper json;
    private final HttpClient http;

    public HttpJsonTransport(URI base, ObjectMapper json) {
        validateBase(base);
        this.base = base;
        this.json = json;
        this.http =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
    }

    public static void validateBase(URI base) {
        if (!"https".equals(base.getScheme())
                || base.getHost() == null
                || base.getUserInfo() != null
                || base.getQuery() != null
                || base.getFragment() != null
                || !List.of("", "/").contains(base.getPath())) {
            throw new Failure("SimpleLogin benötigt eine HTTPS-Basisadresse ohne Pfad.");
        }
    }

    @Override
    public JsonNode request(
            String method, String path, String apiKey, JsonNode body, int expectedStatus) {
        URI target = base.resolve(path);
        if (!base.getAuthority().equals(target.getAuthority())
                || !base.getScheme().equals(target.getScheme())) {
            throw new Failure("SimpleLogin-Anfrage liegt außerhalb des konfigurierten Servers.");
        }
        try {
            var request =
                    HttpRequest.newBuilder(target)
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json");
            if (apiKey != null) request.header("Authentication", apiKey);
            request.method(
                    method,
                    body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != expectedStatus)
                throw new HttpFailure(response.statusCode());
            JsonNode result = json.readTree(response.body());
            if (result == null || !result.isObject())
                throw new Failure("Unerwartete SimpleLogin-Antwort.");
            return result;
        } catch (Failure failure) {
            throw failure;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new Failure(
                    "SimpleLogin-Anfrage abgebrochen; Ergebnis vor Wiederholung im Konto prüfen.");
        }
    }
}
