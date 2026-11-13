package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.application.boundary.cli.Pass2Sec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

class DryRunCommandTest {
    @TempDir Path temp;
    Path binary;
    Path profile;
    StringWriter output;
    StringWriter errors;

    @BeforeEach
    void setup() throws Exception {
        binary = temp.resolve("fake-pass-cli");
        try (var source = getClass().getResourceAsStream("/fake-pass-cli.sh")) {
            Files.copy(source, binary);
        }
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwx------"));
        profile = temp.resolve("profile.json");
        new ObjectMapper().writeValue(profile.toFile(), RotationServiceTest.profile());
    }

    @Test
    void authenticationAndKeygenPreviewsNeverStartProcessesOrCreateState() {
        var paths =
                List.of(
                        List.of("auth", "proton", "login"),
                        List.of("auth", "proton", "logout"),
                        List.of("auth", "simplelogin", "login"),
                        List.of("auth", "simplelogin", "set-key"),
                        List.of("keygen"));
        for (var path : paths) {
            for (boolean before : List.of(true, false)) {
                var args = new ArrayList<String>();
                if (before) args.add("--dry-run");
                args.addAll(path);
                if (!before) args.add("--dry-run");
                assertEquals(0, run(args, "https://preview.invalid"), errors::toString);
                assertTrue(output.toString().contains("DRY-RUN"));
                assertFalse(
                        output.toString()
                                .lines()
                                .anyMatch(line -> line.matches("[A-Za-z0-9+/]{43}=")));
                assertFalse(Files.exists(temp.resolve("state")));
                assertFalse(Files.exists(Path.of(binary + ".calls")));
            }
        }
    }

    @Test
    void explicitDryRunOverridesExecuteAndOnlyReadsProtonEntries() throws Exception {
        var args = rotationArguments();
        args.addAll(List.of("--execute", "--dry-run"));
        assertEquals(0, run(args, "https://preview.invalid"), errors::toString);
        assertRotationOnlyRead();
    }

    @Test
    void globalDryRunBeforeRotateAlsoOverridesExecute() throws Exception {
        var args = new ArrayList<>(List.of("--dry-run"));
        args.addAll(rotationArguments());
        args.add("--execute");
        assertEquals(0, run(args, "https://preview.invalid"), errors::toString);
        assertRotationOnlyRead();
    }

    @Test
    void invalidAliasSettingsAreRejectedBeforeProtonAccess() {
        var args = rotationArguments();
        args.set(args.indexOf("1"), "-1");
        args.add("--dry-run");
        assertEquals(1, run(args, "https://preview.invalid"));
        assertTrue(errors.toString().contains("Mailbox"));
        assertFalse(Files.exists(Path.of(binary + ".calls")));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void authenticationPreviewStillChecksLocalConfiguration() {
        assertEquals(
                1,
                run(
                        List.of("auth", "simplelogin", "login", "--dry-run"),
                        "http://preview.invalid"));
        assertTrue(errors.toString().contains("HTTPS"));
        assertEquals(
                1,
                run(
                        List.of("auth", "simplelogin", "login", "--device", "", "--dry-run"),
                        "https://preview.invalid"));
        assertTrue(errors.toString().contains("Gerätename"));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void statusPreviewWithNoStateDoesNotCreateDirectoryOrAskForKey() {
        assertEquals(
                0,
                run(
                        List.of("status", "--vault", "vault", "--item", "item", "--dry-run"),
                        "https://preview.invalid"));
        assertTrue(output.toString().contains("Kein Vorgang"));
        assertFalse(Files.exists(temp.resolve("state")));
        assertFalse(Files.exists(Path.of(binary + ".calls")));
    }

    @Test
    void recoveryPreviewWithNoStateFailsWithoutCreatingDirectoryOrStartingBrowser() {
        assertEquals(
                1,
                run(
                        List.of(
                                "recover",
                                "--vault",
                                "vault",
                                "--item",
                                "item",
                                "--profile",
                                profile.toString(),
                                "--dry-run"),
                        "https://preview.invalid"));
        assertTrue(errors.toString().contains("Kein lokaler Zustand"));
        assertFalse(Files.exists(temp.resolve("state")));
        assertFalse(Files.exists(Path.of(binary + ".calls")));
    }

    @Test
    void readOnlyAccountListingStillRunsAndClearlyMarksDryRun() throws Exception {
        assertEquals(
                0,
                run(
                        List.of("accounts", "--vault", "vault", "--dry-run"),
                        "https://preview.invalid"));
        assertEquals("item list\n", Files.readString(Path.of(binary + ".calls")));
        assertTrue(output.toString().contains("DRY-RUN"));
        assertTrue(output.toString().contains("Example"));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    private int run(List<String> command, String server) {
        output = new StringWriter();
        errors = new StringWriter();
        var cli = new CommandLine(new Pass2Sec());
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(errors));
        var args =
                new ArrayList<>(
                        List.of(
                                "--pass-cli",
                                binary.toString(),
                                "--state-dir",
                                temp.resolve("state").toString(),
                                "--simplelogin-url",
                                server));
        args.addAll(command);
        return cli.execute(args.toArray(String[]::new));
    }

    private ArrayList<String> rotationArguments() {
        return new ArrayList<>(
                List.of(
                        "rotate",
                        "--vault",
                        "vault",
                        "--item",
                        "item",
                        "--profile",
                        profile.toString(),
                        "--suffix",
                        "@alias.example",
                        "--mailbox",
                        "1"));
    }

    private void assertRotationOnlyRead() throws Exception {
        assertEquals("item list\nitem view\n", Files.readString(Path.of(binary + ".calls")));
        assertFalse(Files.exists(temp.resolve("state")));
        assertTrue(output.toString().contains("DRY-RUN"));
        assertFalse(output.toString().contains("fake-old-secret"));
        assertTrue(output.toString().contains("example@alias.example"));
    }
}
