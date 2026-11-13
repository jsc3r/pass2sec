package de.jsc3r.pass2sec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.TimeUnit;

class LauncherTest {
    @TempDir Path temp;

    @Test
    void installedLinkUsesProjectDirectoryAndPreservesArgumentsAndExitCode() throws Exception {
        Path project = fixture();
        Path link = temp.resolve("installed launcher");
        Files.createSymbolicLink(link, project.resolve("pass2sec"));
        ProcessBuilder builder =
                new ProcessBuilder(link.toString(), "auth", "help", "argument with spaces");
        builder.directory(temp.toFile());
        builder.environment().put("JAVA_HOME", temp.resolve("fake jdk").toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(23, process.exitValue(), output);
        assertEquals(
                List.of(
                        project.toString(),
                        "-jar",
                        project.resolve("target/quarkus-app/quarkus-run.jar").toString(),
                        "auth",
                        "help",
                        "argument with spaces"),
                output.lines().toList());
    }

    @Test
    void invalidJavaHomeFailsWithoutStartingBuildOrApplication() throws Exception {
        Path project = fixture();
        ProcessBuilder builder = new ProcessBuilder(project.resolve("pass2sec").toString());
        builder.environment().put("JAVA_HOME", temp.resolve("missing jdk").toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(1, process.exitValue());
        assertTrue(output.contains("JAVA_HOME"), output);
        assertFalse(output.contains("-jar"), output);
    }

    private Path fixture() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project with spaces"));
        Path launcher = Files.copy(Path.of("pass2sec"), project.resolve("pass2sec"));
        Files.setPosixFilePermissions(launcher, PosixFilePermissions.fromString("rwx------"));
        Files.createDirectories(project.resolve("target/quarkus-app"));
        Files.createFile(project.resolve("target/quarkus-app/quarkus-run.jar"));
        Path java = temp.resolve("fake jdk/bin/java");
        Files.createDirectories(java.getParent());
        Files.writeString(java, "#!/bin/sh\nprintf '%s\\n' \"$PWD\" \"$@\"\nexit 23\n");
        Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwx------"));
        return project;
    }
}
