package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.application.boundary.cli.Pass2Sec;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.boundary.PassCli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

class CliTest {
    @TempDir Path temp;

    Path binary() throws Exception {
        var binary = temp.resolve("fake-pass-cli");
        try (var source = getClass().getResourceAsStream("/fake-pass-cli.sh")) {
            Files.copy(source, binary);
        }
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwx------"));
        return binary;
    }

    @Test
    void dryRunReadsOnlyAndDoesNotCreateJournalOrInvokeGenerator() throws Exception {
        var binary = binary();
        var profile = temp.resolve("example.json");
        new ObjectMapper().writeValue(profile.toFile(), RotationServiceTest.profile());
        int result =
                new CommandLine(new Pass2Sec())
                        .execute(
                                "rotate",
                                "--pass-cli",
                                binary.toString(),
                                "--state-dir",
                                temp.resolve("state").toString(),
                                "--vault",
                                "vault",
                                "--profile",
                                profile.toString(),
                                "--suffix",
                                "@alias.example",
                                "--mailbox",
                                "1");
        assertEquals(0, result);
        assertEquals("item list\nitem view\n", Files.readString(Path.of(binary + ".calls")));
        assertFalse(Files.exists(temp.resolve("state")));
    }

    @Test
    void providerStderrIsNotExposedInFailure() throws Exception {
        var binary = binary();
        var pass = new PassCli(binary.toString(), new ObjectMapper());
        var failure = assertThrows(Failure.class, pass::vaults);
        assertFalse(failure.getMessage().contains("sensitive-provider-error"));
        assertNull(failure.getCause());
    }

    @Test
    void acceptsHumanUpdateAcknowledgementWithoutParsingItAsJson() throws Exception {
        var binary = binary();
        var pass = new PassCli(binary.toString(), new ObjectMapper());
        assertDoesNotThrow(
                () ->
                        pass.update(
                                RotationServiceTest.OLD,
                                "alias@example.org",
                                "fake-secret",
                                false));
        assertEquals("item update\n", Files.readString(Path.of(binary + ".calls")));
    }

    @Test
    void nestedProtonAuthCommandsUseConfiguredBinary() throws Exception {
        var binary = binary();
        var cli = new CommandLine(new Pass2Sec());
        assertEquals(0, cli.execute("auth", "proton", "login", "--pass-cli", binary.toString()));
        assertEquals(0, cli.execute("auth", "proton", "logout", "--pass-cli", binary.toString()));
        assertEquals("login \nlogout \n", Files.readString(Path.of(binary + ".calls")));
    }
}
