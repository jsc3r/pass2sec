package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;

import picocli.CommandLine.Command;

@Command(
        name = "login",
        description = "Proton-Anmeldung über die offizielle CLI starten (Browser/2FA)",
        footer = {
            "Beispiel: pass2sec auth proton login",
            "Mit --dry-run: nur beschreiben; keine CLI, kein Browser und keine neue Sitzung.",
            "Voraussetzung: pass-cli ist installiert; ggf. --pass-cli /pfad/zur/pass-cli setzen.",
            "Den angezeigten Link im Browser öffnen und dort Anmeldung und 2FA abschließen.",
            "Die offizielle CLI verwaltet die Sitzung und fragt ggf. nach dem zusätzlichen"
                    + " Pass-Passwort.",
            "Ein gesetztes PROTON_PASS_PERSONAL_ACCESS_TOKEN wird an pass-cli vererbt.",
            "Während dieses externen Anmeldedialogs gelten die Eingaben der Proton-CLI.",
            "Danach: pass2sec vaults; separat pass2sec auth simplelogin login einrichten.",
            "Zurück zur Übersicht: pass2sec auth proton help oder pass2sec help."
        })
public final class ProtonLoginCommand extends SafeCommand {
    @Override
    protected void preview() {
        describePreview(
                "Würde die konfigurierte Proton-CLI mit login starten und Browser-/2FA-Anmeldung"
                    + " übergeben.",
                "Es wird kein Prozess gestartet, kein Browser geöffnet und keine Sitzung angelegt.",
                "Binary, Anmeldung und Berechtigungen wurden nicht geprüft.");
    }

    @Override
    protected void execute() {
        components().pass().login();
        out().println("Proton-Anmeldung abgeschlossen.");
    }
}
