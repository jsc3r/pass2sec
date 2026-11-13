package de.jsc3r.pass2sec.migration.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;
import de.jsc3r.pass2sec.migration.boundary.Journal;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        description = "Journal-Status ohne Passwörter anzeigen",
        footer = {
            "Beispiel: pass2sec status --vault SHARE_ID --item ITEM_ID",
            "Mit --dry-run: dieselbe lesende Anzeige, als Probelauf gekennzeichnet; keine"
                + " Verzeichnisse oder Sperrdateien anlegen.",
            "Benötigt das ursprüngliche --state-dir und den bisherigen Journal-Schlüssel; keinen"
                    + " Netzwerkzugang.",
            "Zeigt Zustand, Website-Profil und neuen Alias, aber keine Passwörter.",
            "",
            "So geht es je Zustand weiter:",
            "  Kein Vorgang: Migration mit pass2sec help rotate planen.",
            "  PREPARED: Alias-Erstellung war geplant oder unklar; den Zustand in SimpleLogin"
                    + " manuell prüfen.",
            "  ALIAS_READY: Alias existiert, Website-Start noch nicht protokolliert; nicht"
                    + " automatisch erneut starten.",
            "  WEBSITE_STARTED: Website kann teilweise geändert sein; pass2sec help recover lesen.",
            "  WEBSITE_VERIFIED: Website-Verifikation war erfolgreich; recover kann den Tresor"
                    + " abgleichen.",
            "  COMMITTED: Abgeschlossen; derselbe Eintrag wird mit diesem Journal nicht nochmals"
                    + " rotiert.",
            "Bei Teiländerungen Journal und Schlüssel behalten; ggf. manuell abgleichen.",
            "Hilfe während eigener Eingaben: help. Alle Befehle: pass2sec help."
        })
public final class StatusCommand extends SafeCommand {
    @Option(names = "--vault", required = true, description = "Ursprüngliche Share-ID des Tresors")
    private String vault;

    @Option(names = "--item", required = true, description = "Ursprüngliche Item-ID des Vorgangs")
    private String item;

    @Override
    protected void execute() {
        var components = components();
        if (!components.stateExists()) {
            out().println("Kein Vorgang vorhanden; keine lokale Zustandsablage angelegt.");
            return;
        }
        try (var store = components.openReadOnlyState()) {
            var saved = new Journal(store).read(vault, item);
            if (saved.isEmpty()) {
                out().println("Kein Vorgang vorhanden.");
                return;
            }
            var rotation = saved.get();
            out().println(
                            rotation.stage()
                                    + "\t"
                                    + components.display(rotation.profile())
                                    + "\t"
                                    + components.display(rotation.email()));
        }
    }

    @Override
    protected void preview() {
        describePreview(
                "Nur lesend: vorhandenen Journalzustand anzeigen; kein Netzwerkzugriff und keine"
                    + " Schreibzugriffe.");
        execute();
    }
}
