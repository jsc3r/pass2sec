package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;
import de.jsc3r.pass2sec.shared.boundary.HttpJsonTransport;
import de.jsc3r.pass2sec.shared.entity.Failure;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "login",
        description = "Mit E-Mail, Passwort und optional TOTP einen API-Key beziehen",
        footer = {
            "Beispiel: pass2sec auth simplelogin login --device mein-rechner",
            "Mit --dry-run: lokale Optionen prüfen und Schritte beschreiben; keine"
                + " Geheimnisabfrage, HTTP-Anfrage oder Datei.",
            "Vorher: pass2sec keygen; den erzeugten Schlüssel sicher außerhalb des Projekts"
                    + " aufbewahren.",
            "Im echten Terminal: Journal-Schlüssel, SimpleLogin-E-Mail, Passwort und ggf. TOTP"
                    + " eingeben.",
            "Der Geräte-API-Key wird über /api/user_info geprüft und verschlüsselt in .pass2sec/"
                    + " gespeichert.",
            "Passwort und TOTP werden nicht gespeichert; der API-Key wird nicht ausgegeben.",
            "Falls der Passwort-Login wegen FIDO/SSO scheitert: pass2sec auth simplelogin set-key.",
            "Bei Speicherfehlern nach erfolgreichem Login den neuen Geräte-Key im Konto prüfen.",
            "Während eigener Eingaben: help für Hilfe, \\help für den wörtlichen Wert help; Strg+C"
                    + " bricht ab.",
            "Danach: pass2sec vaults und pass2sec help rotate. Alle Befehle: pass2sec help."
        })
public final class SimpleLoginLoginCommand extends SafeCommand {
    @Option(
            names = "--device",
            defaultValue = "pass2sec",
            description = "Erkennbarer Name des erzeugten API-Keys")
    private String device;

    @Override
    protected void execute() {
        validateOptions();
        var components = components();
        try (var store = components.openState()) {
            components.authentication(store).login(components.simpleLoginUrl(), device);
            out().println(
                            "SimpleLogin angemeldet; API-Key validiert und verschlüsselt"
                                    + " gespeichert.");
        }
    }

    @Override
    protected void preview() {
        validateOptions();
        describePreview(
                "Geprüft: HTTPS-Serveradresse und Länge des Gerätenamens.",
                "Würde E-Mail, Passwort und ggf. TOTP abfragen, einen Geräte-API-Key beziehen und"
                    + " validieren.",
                "Würde den API-Key verschlüsselt im konfigurierten --state-dir speichern.",
                "Keine Eingaben, HTTP-Anfragen, Schlüsselgenerierung oder Dateizugriffe auf"
                    + " Zugangsdaten.",
                "Zugangsdaten, Server-Erreichbarkeit und Schreibrechte wurden nicht geprüft.");
    }

    private void validateOptions() {
        if (device.isBlank() || device.length() > 100)
            throw new Failure("Gerätename muss 1–100 Zeichen enthalten.");
        HttpJsonTransport.validateBase(components().simpleLoginUrl());
    }
}
