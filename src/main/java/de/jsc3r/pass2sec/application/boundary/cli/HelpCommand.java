package de.jsc3r.pass2sec.application.boundary.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

/** Read-only navigation of the command tree, including complete nested command paths. */
@Command(
        name = "help",
        helpCommand = true,
        description = "Detaillierte Hilfe, verfügbare Befehle und nächste Schritte anzeigen.",
        footer = {
            "Ohne BEFEHL wird die Hilfe der aktuellen Ebene angezeigt.",
            "Befehlspfade werden zunächst relativ, danach vom Programmeinstieg aus gesucht.",
            "Beispiele: pass2sec help rotate; pass2sec help auth simplelogin login;",
            "          pass2sec auth proton help; pass2sec rotate help.",
            "Hilfe benötigt keine Anmeldung, keine Zugangsdaten und keine Netzwerkverbindung."
        })
public final class HelpCommand
        implements Callable<Integer>, CommandLine.IHelpCommandInitializable2 {
    @Parameters(
            arity = "0..*",
            paramLabel = "BEFEHL",
            description = "Optionaler Befehlspfad, z. B. auth simplelogin login")
    private List<String> path = List.of();

    private CommandLine self;
    private CommandLine.Help.ColorScheme colors;
    private PrintWriter out;

    @Override
    public void init(
            CommandLine command,
            CommandLine.Help.ColorScheme colors,
            PrintWriter out,
            PrintWriter err) {
        this.self = command;
        this.colors = colors;
        this.out = out;
    }

    @Override
    public Integer call() {
        CommandLine parent = self.getParent() == null ? self : self.getParent();
        CommandLine target = resolve(parent, path);
        if (target == null) target = resolve(parent.getCommandSpec().root().commandLine(), path);
        if (target == null) {
            throw new CommandLine.ParameterException(
                    parent,
                    "Unbekannter Hilfe-Befehlspfad. Mit 'pass2sec help' die verfügbaren Befehle"
                            + " anzeigen.");
        }
        target.usage(out, colors);
        out.flush();
        return target.getCommandSpec().exitCodeOnUsageHelp();
    }

    private CommandLine resolve(CommandLine start, List<String> path) {
        CommandLine current = start;
        for (String part : path) {
            current = current.getSubcommands().get(part);
            if (current == null) return null;
        }
        return current;
    }
}
