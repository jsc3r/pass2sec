package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;

import picocli.CommandLine.Command;

@Command(
        name = "logout",
        description = "Offizielle Proton-CLI-Sitzung abmelden",
        footer = {
            "Beispiel: pass2sec auth proton logout",
            "Mit --dry-run: nur beschreiben; eine vorhandene Sitzung wird nicht abgemeldet.",
            "Meldet die Sitzung der konfigurierten pass-cli ab; deren Rückfragen erscheinen im"
                    + " Terminal.",
            "Gespeicherte SimpleLogin-Zugangsdaten und das Wiederherstellungsjournal werden"
                    + " behalten.",
            "Für erneuten Tresorzugriff: pass2sec auth proton login.",
            "Weitere Befehle: pass2sec auth help oder pass2sec help."
        })
public final class ProtonLogoutCommand extends SafeCommand {
    @Override
    protected void preview() {
        describePreview(
                "Würde die konfigurierte Proton-CLI mit logout starten.",
                "Kein Prozess wird gestartet; die vorhandene Sitzung bleibt bestehen.",
                "Ob eine Sitzung besteht und ob das Binary ausführbar ist, wurde nicht geprüft.");
    }

    @Override
    protected void execute() {
        components().pass().logout();
        out().println("Proton-Abmeldung abgeschlossen.");
    }
}
