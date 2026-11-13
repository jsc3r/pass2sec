package de.jsc3r.pass2sec.application.boundary.cli;

import de.jsc3r.pass2sec.shared.entity.Failure;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/** Template Method: consistent exit codes and secret-free error reporting for every command. */
@Command(subcommands = HelpCommand.class)
public abstract class SafeCommand implements Callable<Integer> {
    @Spec private picocli.CommandLine.Model.CommandSpec spec;

    @Override
    public final Integer call() {
        try {
            if (((Pass2Sec) spec.root().userObject()).dryRun()) {
                out().println(
                                "DRY-RUN: Änderungen sind deaktiviert; lesende Prüfungen sind"
                                    + " möglich.");
                preview();
            } else {
                execute();
            }
            return 0;
        } catch (Failure failure) {
            return reportFailure(failure.getMessage());
        } catch (Exception exception) {
            return reportFailure(
                    "Vorgang fehlgeschlagen. Journal behalten; Konfiguration und Zustand prüfen.");
        }
    }

    protected abstract void execute() throws Exception;

    /** Every command must explicitly define its non-mutating behavior. */
    protected abstract void preview() throws Exception;

    protected void describePreview(String... steps) {
        for (String step : steps) out().println("  " + step);
    }

    protected ApplicationComponents components() {
        return ((Pass2Sec) spec.root().userObject())
                .components(
                        () -> {
                            spec.commandLine().usage(out());
                            out().println(
                                            "Alle Befehle: pass2sec help. Zurück zur Eingabe;"
                                                    + " Strg+C bricht den Vorgang ab.");
                            out().flush();
                        });
    }

    protected PrintWriter out() {
        return spec.commandLine().getOut();
    }

    private int reportFailure(String message) {
        var errors = spec.commandLine().getErr();
        errors.println(message);
        errors.println(
                "Hilfe zu diesem Schritt: "
                        + spec.qualifiedName()
                        + " help. Alle Befehle: pass2sec help.");
        errors.flush();
        return 1;
    }
}
