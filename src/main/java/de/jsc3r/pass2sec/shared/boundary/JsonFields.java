package de.jsc3r.pass2sec.shared.boundary;

import com.fasterxml.jackson.databind.JsonNode;

import de.jsc3r.pass2sec.shared.entity.Failure;

public final class JsonFields {
    private JsonFields() {}

    public static JsonNode requiredArray(JsonNode node, String key) {
        JsonNode value = node.path(key);
        if (!value.isArray()) throw new Failure("Unerwartetes JSON-Format: Liste fehlt.");
        return value;
    }

    public static String required(JsonNode node, String key) {
        JsonNode value = node.path(key);
        if (!value.isTextual() || value.asText().isBlank())
            throw new Failure("Unerwartetes JSON-Format: Pflichtfeld fehlt.");
        return value.asText();
    }
}
