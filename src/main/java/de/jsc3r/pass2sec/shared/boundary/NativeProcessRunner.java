package de.jsc3r.pass2sec.shared.boundary;

import de.jsc3r.pass2sec.shared.control.port.ProcessRunner;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class NativeProcessRunner implements ProcessRunner {
    private static final int MAX_OUTPUT_BYTES = 16 * 1024 * 1024;

    @Override
    public String capture(List<String> command, Duration timeout) {
        Process process =
                start(new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var output =
                    executor.submit(
                            () -> process.getInputStream().readNBytes(MAX_OUTPUT_BYTES + 1));
            await(process, timeout);
            byte[] bytes = output.get(5, TimeUnit.SECONDS);
            if (bytes.length > MAX_OUTPUT_BYTES)
                throw new Failure("Proton-CLI-Ausgabe überschreitet das Größenlimit.");
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Failure failure) {
            throw failure;
        } catch (Exception exception) {
            preserveInterrupt(exception);
            throw new Failure("Proton-CLI-Ausgabe konnte nicht gelesen werden.");
        } finally {
            stop(process);
        }
    }

    @Override
    public void discard(List<String> command, Duration timeout) {
        run(
                new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD),
                timeout);
    }

    @Override
    public void interactive(List<String> command, Duration timeout) {
        // Proton owns its authentication prompts and browser/2FA flow.
        run(new ProcessBuilder(command).inheritIO(), timeout);
    }

    private void run(ProcessBuilder builder, Duration timeout) {
        Process process = start(builder);
        try {
            await(process, timeout);
        } finally {
            stop(process);
        }
    }

    private Process start(ProcessBuilder builder) {
        try {
            Process process = builder.start();
            if (builder.redirectInput() == ProcessBuilder.Redirect.PIPE)
                process.getOutputStream().close();
            return process;
        } catch (Exception exception) {
            throw new Failure("Proton-CLI nicht ausführbar; --pass-cli prüfen.");
        }
    }

    private void await(Process process, Duration timeout) {
        try {
            if (!process.waitFor(timeout)) {
                stop(process);
                throw new Failure(
                        "Proton-CLI: Zeitlimit überschritten; Zustand vor Wiederholung prüfen.");
            }
            if (process.exitValue() != 0)
                throw new Failure(
                        "Proton-CLI fehlgeschlagen; Anmeldung und Berechtigungen prüfen.");
        } catch (InterruptedException exception) {
            stop(process);
            Thread.currentThread().interrupt();
            throw new Failure("Proton-CLI-Aufruf unterbrochen.");
        }
    }

    private void preserveInterrupt(Exception exception) {
        if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
    }

    private void stop(Process process) {
        if (process.isAlive()) process.destroyForcibly();
    }
}
