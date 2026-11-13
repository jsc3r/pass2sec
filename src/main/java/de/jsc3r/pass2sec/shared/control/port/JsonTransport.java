package de.jsc3r.pass2sec.shared.control.port;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonTransport {
    JsonNode request(String method, String path, String apiKey, JsonNode body, int expectedStatus);
}
