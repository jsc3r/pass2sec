package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.HelpCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@Command(
        name = "proton",
        description = "Offizielle Proton-CLI-Sitzung verwalten",
        subcommands = {HelpCommand.class, ProtonLoginCommand.class, ProtonLogoutCommand.class},
        footer = {
            "Voraussetzung: Die offizielle Proton Pass CLI ist installiert und als pass-cli"
                    + " verfügbar.",
            "Anderes Binary: --pass-cli /pfad/zur/pass-cli.",
            "  pass2sec auth proton login  Im Browser anmelden und ggf. 2FA abschließen.",
            "  pass2sec auth proton logout Bestehende CLI-Sitzung abmelden.",
            "Proton speichert seine Sitzung selbst; pass2sec speichert kein Proton-Masterpasswort.",
            "Nach dem Login: pass2sec vaults. SimpleLogin separat mit auth simplelogin einrichten.",
            "Details: pass2sec auth proton help login. Alle Befehle: pass2sec help."
        })
public final class ProtonAuthCommand implements Runnable {
    @Spec private picocli.CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
