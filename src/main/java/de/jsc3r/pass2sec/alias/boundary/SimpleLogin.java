package de.jsc3r.pass2sec.alias.boundary;

import static de.jsc3r.pass2sec.shared.boundary.JsonFields.*;

import com.fasterxml.jackson.databind.*;

import de.jsc3r.pass2sec.alias.control.port.Aliases;
import de.jsc3r.pass2sec.alias.entity.AliasDestination;
import de.jsc3r.pass2sec.alias.entity.AliasPlan;
import de.jsc3r.pass2sec.shared.boundary.HttpJsonTransport;
import de.jsc3r.pass2sec.shared.control.port.JsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimpleLogin implements Aliases {
    private final String apiKey;
    private final ObjectMapper json;
    private final JsonTransport transport;

    public SimpleLogin(URI base, String apiKey, ObjectMapper json) {
        this(new HttpJsonTransport(base, json), apiKey, json);
    }

    public SimpleLogin(JsonTransport transport, String apiKey, ObjectMapper json) {
        if (apiKey == null || apiKey.isBlank()) throw new Failure("SIMPLELOGIN_API_KEY fehlt.");
        this.transport = transport;
        this.apiKey = apiKey;
        this.json = json;
    }

    @Override
    public AliasPlan plan(String host, String prefix, String suffix, List<Long> mailboxes) {
        new AliasDestination(suffix, mailboxes);
        if (!prefix.matches("[a-z0-9][a-z0-9._-]{0,63}"))
            throw new Failure("Alias-Präfix ungültig.");
        var options =
                transport.request(
                        "GET", "/api/v5/alias/options?hostname=" + encode(host), apiKey, null, 200);
        if (!options.path("can_create").asBoolean())
            throw new Failure("SimpleLogin erlaubt keinen weiteren Alias.");
        for (var s : requiredArray(options, "suffixes"))
            if (suffix.equals(s.path("suffix").asText()))
                return new AliasPlan(host, prefix, suffix, required(s, "signed_suffix"), mailboxes);
        throw new Failure(
                "Gewünschte Alias-Endung steht diesem SimpleLogin-Konto nicht zur Verfügung.");
    }

    @Override
    public void create(AliasPlan plan) {
        var body =
                json.createObjectNode()
                        .put("alias_prefix", plan.prefix())
                        .put("signed_suffix", plan.signedSuffix())
                        .put("note", "pass2sec: " + plan.host());
        body.set("mailbox_ids", json.valueToTree(plan.mailboxes()));
        var response =
                transport.request(
                        "POST",
                        "/api/v3/alias/custom/new?hostname=" + encode(plan.host()),
                        apiKey,
                        body,
                        201);
        if (!plan.email().equalsIgnoreCase(required(response, "email")))
            throw new Failure(
                    "SimpleLogin lieferte einen unerwarteten Alias; Journal und Konto prüfen.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
