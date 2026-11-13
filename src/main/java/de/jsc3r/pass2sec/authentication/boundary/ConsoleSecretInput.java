package de.jsc3r.pass2sec.authentication.boundary;

import de.jsc3r.pass2sec.authentication.control.port.SecretInput;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.io.Console;

public final class ConsoleSecretInput implements SecretInput {
    @Override
    public String readLine(String prompt) {
        String value = console().readLine("%s", prompt);
        if (value == null || value.isBlank()) throw new Failure("Eingabe abgebrochen oder leer.");
        return value;
    }

    @Override
    public char[] readSecret(String prompt) {
        char[] value = console().readPassword("%s", prompt);
        if (value == null || value.length == 0)
            throw new Failure("Geheime Eingabe abgebrochen oder leer.");
        return value;
    }

    private Console console() {
        Console console = System.console();
        if (console == null) {
            throw new Failure(
                    "Für geheime Eingaben ein echtes Terminal verwenden; alternativ"
                            + " Umgebungsvariablen setzen.");
        }
        return console;
    }
}
