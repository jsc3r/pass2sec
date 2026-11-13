package de.jsc3r.pass2sec;

import com.fasterxml.jackson.databind.JsonNode;

import de.jsc3r.pass2sec.shared.control.port.JsonTransport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class FakeJsonTransport implements JsonTransport {
    final Deque<Object> responses = new ArrayDeque<>();
    final List<String> methods = new ArrayList<>();
    final List<String> paths = new ArrayList<>();
    final List<String> apiKeys = new ArrayList<>();
    final List<JsonNode> bodies = new ArrayList<>();

    @Override
    public JsonNode request(String method, String path, String key, JsonNode body, int status) {
        methods.add(method);
        paths.add(path);
        apiKeys.add(key);
        bodies.add(body == null ? null : body.deepCopy());
        Object response = responses.removeFirst();
        if (response instanceof RuntimeException failure) throw failure;
        return (JsonNode) response;
    }
}
