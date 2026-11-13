package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.HelpCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@Command(
        name = "simplelogin",
        description = "SimpleLogin-Zugang verschlüsselt einrichten",
        subcommands = {
            HelpCommand.class,
            SimpleLoginLoginCommand.class,
            SimpleLoginSetKeyCommand.class
        },
        footer = {
            "Vorbereitung: pass2sec keygen; den erzeugten Schlüssel separat sicher aufbewahren.",
            "  pass2sec auth simplelogin login --device pass2sec",
            "    E-Mail, Passwort und optional TOTP eingeben; ein Geräte-API-Key wird erzeugt.",
            "  pass2sec auth simplelogin set-key",
            "    Einen bereits vorhandenen API-Key verdeckt einlesen (z. B. für FIDO-/SSO-Konten).",
            "Der API-Key wird geprüft und verschlüsselt in .pass2sec/ im Projekt gespeichert.",
            "Den Journal-Schlüssel immer beibehalten; ohne ihn ist die Datei nicht lesbar.",
            "Eigener Server: bei Einrichtung und Migration denselben --simplelogin-url verwenden.",
            "Nach der Einrichtung: pass2sec help rotate. Alle Befehle: pass2sec help.",
            "In eigenen Eingabeabfragen zeigt help die Hilfe und fragt anschließend erneut."
        })
public final class SimpleLoginAuthCommand implements Runnable {
    @Spec private picocli.CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
