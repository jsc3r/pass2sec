package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import de.jsc3r.pass2sec.application.boundary.cli.InteractiveShell;
import de.jsc3r.pass2sec.application.boundary.cli.Pass2Sec;
import de.jsc3r.pass2sec.application.boundary.cli.ShellArguments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

class InteractiveShellTest {
    @TempDir Path temp;

    @Test
    void acceptsMultipleCommandsAndKeepsRunningAfterInvalidCommands() {
        var output = new StringWriter();
        var errors = new StringWriter();
        run("\nhelp\nauth help\nunknown-command\nkeygen --dry-run\nexit\nkeygen\n", output, errors);
        assertTrue(output.toString().contains("Verfügbare Befehle"));
        assertTrue(output.toString().contains("Proton und SimpleLogin"));
        assertTrue(output.toString().contains("DRY-RUN"));
        assertTrue(errors.toString().contains("Unmatched argument"));
        assertEquals(6, output.toString().split("pass2sec> ", -1).length - 1);
        assertFalse(output.toString().lines().anyMatch(line -> line.matches("[A-Za-z0-9+/]{43}=")));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void commandOptionsNeverLeakIntoTheNextInvocation() {
        var output = new StringWriter();
        var errors = new StringWriter();
        run("auth proton logout --dry-run\nkeygen --help\nquit\n", output, errors);
        assertEquals(1, output.toString().split("DRY-RUN:", -1).length - 1);
        assertTrue(output.toString().contains("Journal-Schlüssel"));
        assertEquals("", errors.toString());
    }

    @Test
    void dryRunIsNotStickyButSessionDryRunRemainsActive() throws Exception {
        Path fake = temp.resolve("fake-pass-cli");
        try (var source = getClass().getResourceAsStream("/fake-pass-cli.sh")) {
            Files.copy(source, fake);
        }
        Files.setPosixFilePermissions(fake, PosixFilePermissions.fromString("rwx------"));
        String command = "auth proton logout --pass-cli '" + fake + "'";
        var output = new StringWriter();
        var errors = new StringWriter();
        run(command + " --dry-run\n" + command + "\nexit\n", output, errors);
        assertEquals("", errors.toString());
        assertEquals("logout \n", Files.readString(Path.of(fake + ".calls")));
        Files.delete(Path.of(fake + ".calls"));

        var input = new BufferedReader(new StringReader(command + "\nexit\n"));
        new InteractiveShell(
                        input::readLine,
                        new PrintWriter(output),
                        new PrintWriter(errors),
                        () -> new CommandLine(new Pass2Sec()),
                        List.of("--dry-run"))
                .run();
        assertFalse(Files.exists(Path.of(fake + ".calls")));
        assertEquals("", errors.toString());
    }

    @Test
    void explicitCommandOptionsOverrideSessionDefaults() {
        var output = new StringWriter();
        var errors = new StringWriter();
        var input =
                new BufferedReader(
                        new StringReader(
                                "auth simplelogin login --simplelogin-url https://preview.invalid"
                                    + " --dry-run\n"
                                    + "exit\n"));
        new InteractiveShell(
                        input::readLine,
                        new PrintWriter(output),
                        new PrintWriter(errors),
                        () -> new CommandLine(new Pass2Sec()),
                        List.of("--simplelogin-url", "http://invalid.example"))
                .run();
        assertEquals("", errors.toString());
        assertTrue(output.toString().contains("DRY-RUN"));
    }

    @Test
    void supportsProgramPrefixQuotesAndEofWithoutStoringHistoryOrState() {
        var output = new StringWriter();
        var errors = new StringWriter();
        run("pass2sec auth simplelogin login --device 'test device' --dry-run\n", output, errors);
        assertTrue(output.toString().contains("DRY-RUN:"));
        assertEquals("", errors.toString());
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void malformedQuotesDoNotExposeTheInputAndDoNotEndSession() {
        var output = new StringWriter();
        var errors = new StringWriter();
        run("auth 'synthetic-sensitive-input\nhelp auth\nexit\n", output, errors);
        assertTrue(errors.toString().contains("Anführungszeichen"));
        assertFalse(errors.toString().contains("synthetic-sensitive-input"));
        assertTrue(output.toString().contains("Proton und SimpleLogin"));
    }

    @Test
    void tokenizerSupportsEmptyAndEscapedArgumentsWithoutShellExpansion() {
        assertEquals(
                List.of("one", "two words", "", "a b", "$VALUE", "$(example)", "a\\b"),
                ShellArguments.parse("one \"two words\" '' a\\ b '$VALUE' '$(example)' 'a\\b'"));
        assertThrows(IllegalArgumentException.class, () -> ShellArguments.parse("unfinished\\"));
    }

    private void run(String script, StringWriter output, StringWriter errors) {
        var input = new BufferedReader(new StringReader(script));
        new InteractiveShell(
                        input::readLine,
                        new PrintWriter(output),
                        new PrintWriter(errors),
                        () -> new CommandLine(new Pass2Sec()),
                        List.of("--state-dir", temp.resolve("state").toString()))
                .run();
    }
}
