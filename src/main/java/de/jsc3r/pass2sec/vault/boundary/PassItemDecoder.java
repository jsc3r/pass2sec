package de.jsc3r.pass2sec.vault.boundary;

import static de.jsc3r.pass2sec.shared.boundary.JsonFields.*;

import com.fasterxml.jackson.databind.JsonNode;

import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.entity.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PassItemDecoder {
    public Account decode(String vault, String id, JsonNode root) {
        JsonNode item = root.path("item");
        if (!id.equals(required(item, "id")) || !vault.equals(required(item, "share_id"))) {
            throw new Failure("Proton lieferte einen anderen Eintrag als angefordert.");
        }
        JsonNode data = item.path("content");
        JsonNode login = loginContent(data.path("content"));
        rejectShadowedFields(data);
        var urls = new ArrayList<String>();
        for (var url : requiredArray(login, "urls")) urls.add(url.asText());
        return new Account(
                vault,
                id,
                required(data, "title"),
                login.path("username").asText(""),
                login.path("email").asText(""),
                required(login, "password"),
                List.copyOf(urls));
    }

    private JsonNode loginContent(JsonNode content) {
        if (content.path("Login").isObject()) return content.path("Login");
        if ("login".equalsIgnoreCase(content.path("type").asText())
                && content.path("key").isObject()) {
            return content.path("key");
        }
        throw new Failure("Kein unterstützter Login-Eintrag im Proton-JSON.");
    }

    private void rejectShadowedFields(JsonNode data) {
        for (var extra : data.path("extra_fields")) {
            if (Set.of("email", "username", "password")
                    .contains(extra.path("name").asText().toLowerCase(Locale.ROOT))) {
                throw new Failure(
                        "Benutzerdefiniertes Feld überschattet ein Login-Feld; vor Migration"
                                + " bereinigen.");
            }
        }
    }
}
