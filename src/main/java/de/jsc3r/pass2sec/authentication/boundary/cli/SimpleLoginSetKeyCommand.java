package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;
import de.jsc3r.pass2sec.shared.boundary.HttpJsonTransport;

import picocli.CommandLine.Command;

@Command(
        name = "set-key",
        description = "Vorhandenen API-Key verdeckt einlesen, prüfen und verschlüsselt speichern",
        footer = {
            "Beispiel: pass2sec auth simplelogin set-key",
            "Mit --dry-run: Serveradresse prüfen und Schritte beschreiben; keinen Key abfragen,"
                + " validieren oder speichern.",
            "Vorher: Einen API-Key im SimpleLogin-Konto und mit pass2sec keygen einen lokalen"
                    + " Schlüssel erzeugen.",
            "Journal-Schlüssel und API-Key im echten Terminal verdeckt eingeben; keine Secrets als"
                    + " Argumente angeben.",
            "Der Key wird validiert und verschlüsselt unter .pass2sec/ gespeichert. Ein ungültiger"
                    + " Key ersetzt keinen gültigen.",
            "Für einen eigenen Server denselben --simplelogin-url bei Einrichtung und Migration"
                    + " verwenden.",
            "SIMPLELOGIN_API_KEY überschreibt bei späteren Migrationen den gespeicherten Key.",
            "Während eigener Eingaben: help für Hilfe, \\help für den wörtlichen Wert help; Strg+C"
                    + " bricht ab.",
            "Danach: pass2sec help rotate. Alle Befehle: pass2sec help."
        })
public final class SimpleLoginSetKeyCommand extends SafeCommand {
    @Override
    protected void preview() {
        HttpJsonTransport.validateBase(components().simpleLoginUrl());
        describePreview(
                "Geprüft: HTTPS-Serveradresse.",
                "Würde Journal-Schlüssel und vorhandenen API-Key verdeckt abfragen und den API-Key"
                    + " validieren.",
                "Würde den API-Key verschlüsselt im konfigurierten --state-dir speichern.",
                "Keine Eingaben, HTTP-Anfragen oder Dateizugriffe auf Zugangsdaten.",
                "Gültigkeit des API-Keys, Server-Erreichbarkeit und Schreibrechte wurden nicht"
                    + " geprüft.");
    }

    @Override
    protected void execute() {
        var components = components();
        try (var store = components.openState()) {
            components.authentication(store).configureApiKey(components.simpleLoginUrl());
            out().println("SimpleLogin-API-Key validiert und verschlüsselt gespeichert.");
        }
    }
}
