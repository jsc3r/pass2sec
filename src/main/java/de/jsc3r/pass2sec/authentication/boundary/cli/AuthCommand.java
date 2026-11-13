package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.HelpCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@Command(
        name = "auth",
        description = "Anmeldung und verschlüsselte API-Key-Einrichtung",
        subcommands = {HelpCommand.class, ProtonAuthCommand.class, SimpleLoginAuthCommand.class},
        footer = {
            "Proton und SimpleLogin werden getrennt angemeldet; ein Login ersetzt den anderen"
                    + " nicht.",
            "  pass2sec auth proton login      Offiziellen Proton-Browserlogin starten.",
            "  pass2sec auth proton logout     Die von pass-cli verwaltete Sitzung abmelden.",
            "  pass2sec auth simplelogin login API-Key mit E-Mail, Passwort und ggf. TOTP"
                    + " beziehen.",
            "  pass2sec auth simplelogin set-key Vorhandenen SimpleLogin-Key verdeckt einlesen.",
            "Vor lokaler Speicherung: pass2sec keygen ausführen und den Schlüssel sicher"
                    + " aufbewahren.",
            "Danach: pass2sec vaults und pass2sec help rotate.",
            "Details: pass2sec auth help simplelogin login; alle Befehle: pass2sec help."
        })
public final class AuthCommand implements Runnable {
    @Spec private picocli.CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
