package de.jsc3r.pass2sec.migration.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;
import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.control.RecoveryService;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.migration.entity.VaultUpdateState;
import de.jsc3r.pass2sec.shared.entity.Failure;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(
        name = "recover",
        description = "Offenen Vorgang verifizieren und Tresor abgleichen",
        footer = {
            "Vorher den Zustand prüfen: pass2sec status --vault SHARE_ID --item ITEM_ID.",
            "Mit --dry-run: Journal und ggf. Tresorwerte nur lesen, Abgleich planen; kein Browser"
                + " und keine Schreibzugriffe.",
            "Auch im Probelauf sind vorhandenes Journal und bisheriger Journal-Schlüssel nötig; die"
                + " Website wird nicht verifiziert.",
            "Beispiel: pass2sec recover --vault SHARE_ID --item ITEM_ID --profile"
                    + " profiles/mein-dienst.json",
            "Benötigt denselben Journal-Schlüssel, dieselbe --state-dir-Ablage, Proton-Zugriff und"
                    + " das passende Website-Profil.",
            "Profilname und updateUsername müssen zum gespeicherten Vorgang passen; Selektoren"
                    + " können korrigiert werden.",
            "",
            "Ab WEBSITE_STARTED: frischen Login mit den neuen Daten und die neue E-Mail prüfen,"
                    + " danach Tresor abgleichen.",
            "Es wird kein neuer Alias erzeugt und der konfigurierte Website-Änderungsablauf nicht"
                    + " wiederholt.",
            "Ein erfolgreicher, aber zuvor nicht bestätigter Tresor-Write wird beim Nachlesen"
                    + " erkannt.",
            "Bei PREPARED/ALIAS_READY oder unvollständigen Website-Änderungen ist manueller"
                    + " Abgleich erforderlich.",
            "recover setzt keine Passwörter zurück und repariert keine Teiländerungen automatisch.",
            "--headless nur bei Profilen ohne PAUSE; Journal und Schlüssel auch nach Fehlern"
                    + " behalten.",
            "Während eigener Eingaben: help für Hilfe; Strg+C bricht ab. Alle Befehle: pass2sec"
                    + " help."
        })
public final class RecoverCommand extends SafeCommand {
    @Option(names = "--vault", required = true, description = "Ursprüngliche Share-ID des Tresors")
    private String vault;

    @Option(names = "--item", required = true, description = "Item-ID des offenen Vorgangs")
    private String item;

    @Option(names = "--profile", required = true, description = "Passendes Website-Profil als JSON")
    private Path path;

    @Option(names = "--headless", description = "Ohne Browserfenster; keine PAUSE-Schritte erlaubt")
    private boolean headless;

    @Override
    protected void execute() {
        var components = components();
        var profile = components.profiles().load(path);
        try (var store = components.openState()) {
            new RecoveryService(components.pass(), components.website(), new Journal(store))
                    .recover(vault, item, profile, headless);
            out().println("Wiederherstellung abgeschlossen.");
        }
    }

    @Override
    protected void preview() {
        var components = components();
        var profile = components.profiles().load(path);
        try (var store = components.openReadOnlyState()) {
            var plan =
                    new RecoveryService(components.pass(), components.website(), new Journal(store))
                            .preview(vault, item, profile, headless);
            out().println(
                            "Journalzustand: "
                                    + plan.stage()
                                    + "; Tresorvergleich: "
                                    + plan.vaultState());
            describePreview(
                    "Geprüft: Journal, passendes Profil und gegebenenfalls der aktuelle"
                        + " Tresoreintrag (nur lesend).",
                    "Keine Website-Verifikation, kein Browserstart und keine lokalen oder"
                        + " entfernten Schreibzugriffe.");
            if (plan.stage() == Stage.COMMITTED) {
                out().println(
                                "Laut Journal bereits abgeschlossen; würde übersprungen. Tresor"
                                    + " wurde nicht erneut geprüft.");
                return;
            }
            if (plan.stage() == Stage.PREPARED || plan.stage() == Stage.ALIAS_READY)
                throw new Failure(
                        "Wiederherstellung blockiert: vor Website-Start abgebrochen; manueller"
                            + " Abgleich erforderlich.");
            if (plan.vaultState() == VaultUpdateState.CONFLICT)
                throw new Failure(
                        "Wiederherstellung blockiert: Tresorwerte weichen ab; kein Überschreiben.");
            describePreview(
                    "Würde zuerst einen frischen Website-Login und die neue E-Mail verifizieren.",
                    plan.vaultState() == VaultUpdateState.UPDATED
                            ? "Würde danach den bereits aktualisierten Tresor erkennen und das"
                                  + " Journal abschließen."
                            : "Würde nur bei erfolgreicher Website-Verifikation den Tresor"
                                  + " aktualisieren und das Journal abschließen.",
                    "Ob die neuen Website-Zugangsdaten funktionieren, wurde nicht geprüft.");
        }
    }
}
