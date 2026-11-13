package de.jsc3r.pass2sec.application.boundary.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Keeps the session alive, but isolates Picocli option state for every command. */
public final class InteractiveShell {
    private final ShellInput input;
    private final PrintWriter out;
    private final PrintWriter err;
    private final Supplier<CommandLine> commands;
    private final List<String> defaults;

    public InteractiveShell(
            ShellInput input,
            PrintWriter out,
            PrintWriter err,
            Supplier<CommandLine> commands,
            List<String> defaults) {
        this.input = input;
        this.out = out;
        this.err = err;
        this.commands = commands;
        this.defaults = List.copyOf(defaults);
    }

    public void run() {
        out.println("pass2sec – interaktive Sitzung");
        out.println(
                "Befehle direkt eingeben, z. B. help oder auth help. Beenden: exit, quit oder"
                    + " Strg+D.");
        out.println(
                "Keine Befehlshistorie; Geheimnisse nur in den verdeckten Eingabeabfragen"
                    + " eingeben.");
        while (true) {
            out.print("pass2sec> ");
            out.flush();
            String line;
            try {
                line = input.readLine();
            } catch (IOException exception) {
                err.println("Terminaleingabe fehlgeschlagen. Sitzung beendet.");
                err.flush();
                return;
            }
            if (line == null) {
                out.println();
                out.flush();
                return;
            }
            List<String> arguments;
            try {
                arguments = ShellArguments.parse(line);
            } catch (IllegalArgumentException exception) {
                err.println(exception.getMessage() + " Hilfe: help.");
                err.flush();
                continue;
            }
            if (arguments.isEmpty()) continue;
            // Accept pasted examples with the optional program name, too.
            if (arguments.getFirst().equals("pass2sec"))
                arguments = arguments.subList(1, arguments.size());
            if (arguments.size() == 1 && List.of("exit", "quit").contains(arguments.getFirst()))
                return;
            var invocation = new ArrayList<>(defaults);
            invocation.addAll(arguments);
            CommandLine command = commands.get();
            // Explicit per-command values override session defaults.
            command.setOverwrittenOptionsAllowed(true);
            command.setOut(out);
            command.setErr(err);
            command.execute(invocation.toArray(String[]::new));
            out.flush();
            err.flush();
        }
    }
}
