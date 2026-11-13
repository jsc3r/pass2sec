package de.jsc3r.pass2sec.authentication.boundary;

import de.jsc3r.pass2sec.authentication.control.port.SecretInput;

import java.util.Arrays;

/** Decorator: help never becomes a login request, API key, password or journal key. */
public final class HelpAwareSecretInput implements SecretInput {
    private final SecretInput delegate;
    private final Runnable showHelp;

    public HelpAwareSecretInput(SecretInput delegate, Runnable showHelp) {
        this.delegate = delegate;
        this.showHelp = showHelp;
    }

    @Override
    public String readLine(String prompt) {
        while (true) {
            String value = delegate.readLine(promptWithHelp(prompt));
            if ("help".equalsIgnoreCase(value.strip())) {
                showHelp.run();
                continue;
            }
            return "\\help".equalsIgnoreCase(value) ? value.substring(1) : value;
        }
    }

    @Override
    public char[] readSecret(String prompt) {
        while (true) {
            char[] value = delegate.readSecret(promptWithHelp(prompt));
            if (isHelp(value, 0)) {
                Arrays.fill(value, '\0');
                showHelp.run();
                continue;
            }
            if (value.length == 5 && value[0] == '\\' && isHelp(value, 1)) {
                char[] literal = Arrays.copyOfRange(value, 1, value.length);
                Arrays.fill(value, '\0');
                return literal;
            }
            return value;
        }
    }

    private boolean isHelp(char[] value, int offset) {
        if (value.length - offset != 4) return false;
        return Character.toLowerCase(value[offset]) == 'h'
                && Character.toLowerCase(value[offset + 1]) == 'e'
                && Character.toLowerCase(value[offset + 2]) == 'l'
                && Character.toLowerCase(value[offset + 3]) == 'p';
    }

    private String promptWithHelp(String prompt) {
        return prompt.replaceFirst(":\\s*$", "").stripTrailing()
                + " [help für Hilfe, Strg+C zum Abbrechen]: ";
    }
}
