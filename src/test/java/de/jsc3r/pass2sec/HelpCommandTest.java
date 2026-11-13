package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import de.jsc3r.pass2sec.application.boundary.cli.HelpCommand;
import de.jsc3r.pass2sec.application.boundary.cli.Pass2Sec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class HelpCommandTest {
    @TempDir Path temp;

    @Test
    void germanLocaleSupportsHelpAndVersionAtEveryLevelWithoutResourceBundle() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            var root = new CommandLine(new Pass2Sec());
            for (List<String> path : commandPaths(root, List.of())) {
                CommandLine command = root;
                for (String name : path) command = command.getSubcommands().get(name);
                assertNull(command.getCommandSpec().resourceBundle(), path.toString());
                var helpArgs = new ArrayList<>(path);
                helpArgs.add("--help");
                assertTrue(runHelp(helpArgs).contains("Diese Hilfe anzeigen und beenden."));
                for (String version : List.of("--version", "-V")) {
                    var versionArgs = new ArrayList<>(path);
                    versionArgs.add(version);
                    assertEquals("pass2sec 1.0", runHelp(versionArgs).strip());
                }
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void helpWorksAtEveryCommandLevelWithoutRequiredOptionsOrSideEffects() {
        for (List<String> path : commandPaths(new CommandLine(new Pass2Sec()), List.of())) {
            for (String help : List.of("help", "--help", "-h")) {
                var args = new ArrayList<>(path);
                args.add(help);
                String output = runHelp(args);
                assertTrue(output.contains("Beispiele und nächste Schritte"), args.toString());
                assertTrue(output.contains("pass2sec help"), args.toString());
                assertTrue(output.contains("Diese Hilfe anzeigen und beenden."), args.toString());
            }
        }
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void helpCanNavigateEntireNestedPathFromRoot() {
        String output = runHelp(List.of("help", "auth", "simplelogin", "login"));
        assertTrue(output.contains("pass2sec auth simplelogin login"));
        assertTrue(output.contains("--device"));
        assertTrue(output.contains("TOTP"));
        assertTrue(output.contains("Journal-Schlüssel"));
        assertTrue(output.contains("verschlüsselt"));
    }

    @Test
    void helpResolvesRelativeAndGlobalPathsFromNestedContexts() {
        assertTrue(
                runHelp(List.of("auth", "help", "proton", "login")).contains("Proton-Anmeldung"));
        assertTrue(runHelp(List.of("auth", "proton", "help", "rotate")).contains("--execute"));
        assertTrue(
                runHelp(List.of("auth", "proton", "login", "help", "status"))
                        .contains("WEBSITE_VERIFIED"));
    }

    @Test
    void helpWithExecutionFlagNeverStartsMigration() {
        String output = runHelp(List.of("rotate", "--execute", "help"));
        assertTrue(
                output.replaceAll("\\s+", " ")
                        .contains("Ohne --execute wird nur gelesen und geplant"));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void helpForKeygenDoesNotGenerateAKey() {
        String output = runHelp(List.of("keygen", "help"));
        assertFalse(output.lines().anyMatch(line -> line.matches("[A-Za-z0-9+/]{43}=")));
    }

    @Test
    void unknownHelpPathExplainsHowToFindAvailableCommands() {
        var cli = new CommandLine(new Pass2Sec());
        var output = new StringWriter();
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(output));
        assertEquals(2, cli.execute("help", "not-a-command"));
        assertTrue(output.toString().contains("pass2sec help"));
        assertFalse(output.toString().contains("Exception"));
    }

    private String runHelp(List<String> command) {
        var cli = new CommandLine(new Pass2Sec());
        var output = new StringWriter();
        var errors = new StringWriter();
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(errors));
        var args =
                new ArrayList<>(
                        List.of(
                                "--state-dir",
                                temp.resolve("state").toString(),
                                "--pass-cli",
                                temp.resolve("does-not-exist").toString()));
        args.addAll(command);
        assertEquals(0, cli.execute(args.toArray(String[]::new)), command + "\n" + errors);
        assertTrue(errors.toString().isEmpty(), errors::toString);
        return output.toString();
    }

    private List<List<String>> commandPaths(CommandLine command, List<String> path) {
        var result = new ArrayList<List<String>>();
        result.add(path);
        command.getSubcommands()
                .forEach(
                        (name, child) -> {
                            if (child.getCommand() instanceof HelpCommand) return;
                            var childPath = new ArrayList<>(path);
                            childPath.add(name);
                            result.addAll(commandPaths(child, childPath));
                        });
        return result;
    }
}
