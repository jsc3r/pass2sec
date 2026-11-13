package de.jsc3r.pass2sec.application.boundary.cli;

import java.util.ArrayList;
import java.util.List;

/** Tokenizes arguments only: no OS shell, substitutions, scripts or environment expansion. */
public final class ShellArguments {
    private ShellArguments() {}

    public static List<String> parse(String line) {
        var result = new ArrayList<String>();
        var token = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean started = false;
        for (char character : line.toCharArray()) {
            if (escaped) {
                token.append(character);
                escaped = false;
            } else if (character == '\\' && quote != '\'') {
                escaped = true;
                started = true;
            } else if (quote != 0) {
                if (character == quote) quote = 0;
                else token.append(character);
            } else if (character == '\'' || character == '"') {
                quote = character;
                started = true;
            } else if (Character.isWhitespace(character)) {
                if (started) {
                    result.add(token.toString());
                    token.setLength(0);
                    started = false;
                }
            } else {
                token.append(character);
                started = true;
            }
        }
        if (escaped || quote != 0)
            throw new IllegalArgumentException(
                    "Unvollständige Anführungszeichen oder Escape-Sequenz.");
        if (started) result.add(token.toString());
        return List.copyOf(result);
    }
}
