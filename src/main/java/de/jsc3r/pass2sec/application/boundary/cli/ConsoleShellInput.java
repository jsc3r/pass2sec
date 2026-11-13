package de.jsc3r.pass2sec.application.boundary.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** No history and no buffering ahead of the console's subsequent secret prompts. */
public final class ConsoleShellInput implements ShellInput {
    private final Console console = System.console();
    private final BufferedReader fallback =
            console == null
                    ? new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
                    : null;

    @Override
    public String readLine() throws IOException {
        return console == null ? fallback.readLine() : console.readLine();
    }
}
