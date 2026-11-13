package de.jsc3r.pass2sec.migration.boundary.cli;

import de.jsc3r.pass2sec.alias.entity.AliasDestination;
import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;
import de.jsc3r.pass2sec.migration.boundary.Journal;
import de.jsc3r.pass2sec.migration.control.RotationPlanner;
import de.jsc3r.pass2sec.migration.control.RotationService;
import de.jsc3r.pass2sec.migration.entity.MigrationPlan;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;

@Command(
        name = "rotate",
        description = "Migration planen; mit --execute ausführen",
        footer = {
            "Voraussetzungen: Proton-Anmeldung, für echte Änderungen zusätzlich SimpleLogin-Zugang"
                    + " und Journal-Schlüssel.",
            "Share-ID mit pass2sec vaults und Item-ID mit pass2sec accounts --vault SHARE_ID"
                    + " ermitteln.",
            "Ein passendes Website-Profil erstellen; profiles/example.json ist eine fiktive"
                    + " Vorlage, kein fertiger GitHub-Adapter.",
            "Für Browser-Abläufe Chromium installieren (Anleitung in README.md).",
            "",
            "Zunächst einen einzelnen Account planen:",
            "  pass2sec rotate --vault SHARE_ID --item ITEM_ID \\",
            "    --profile profiles/mein-dienst.json \\",
            "    --suffix @meine-domain.de --mailbox 123",
            "Alle Beispielargumente gehören in denselben Aufruf. Ohne --execute wird nur gelesen"
                    + " und geplant.",
            "Der Plan bestätigt weder die Verfügbarkeit des Alias noch die Website-Selektoren.",
            "Für echte Änderungen denselben Aufruf um --execute ergänzen.",
            "--dry-run erzwingt die Planung und hat auch zusammen mit --execute Vorrang.",
            "Der Plan liest Proton-Einträge, aber keine lokalen Zugangsdaten oder Journalstände.",
            "",
            "--suffix muss eine exakt verfügbare SimpleLogin-Endung sein, z. B. @eigene-domain.de"
                    + " oder .wort@aleeas.com.",
            "--mailbox erwartet die numerische SimpleLogin-Mailbox-ID, keine E-Mail-Adresse;"
                    + " mehrere IDs mit Komma trennen.",
            "Ohne --item werden alle aktiven Logins des Tresors berücksichtigt. --profile kann"
                    + " mehrfach vorkommen.",
            "Accounts ohne passendes Profil werden übersprungen; mehrdeutige Profile und doppelte"
                    + " Aliasziele führen zum Abbruch.",
            "--headless geht nur ohne PAUSE-Schritte. Für 2FA und Mailbestätigung kann Interaktion"
                    + " nötig sein.",
            "Nach frischem Kontroll-Login und Prüfung der neuen E-Mail wird der Tresoreintrag"
                    + " aktualisiert.",
            "",
            "Nach Abbruch zuerst: pass2sec status --vault SHARE_ID --item ITEM_ID.",
            "Danach ggf. pass2sec help recover. Offene Vorgänge nicht durch neues rotate oder"
                    + " Journal-Löschen umgehen.",
            "Während eigener Schlüsselabfragen: help für Hilfe; Strg+C bricht ab. Alle Befehle:"
                    + " pass2sec help."
        })
public final class RotateCommand extends SafeCommand {
    @Option(names = "--vault", required = true, description = "Share-ID des Tresors")
    private String vault;

    @Option(names = "--item", description = "Optional auf eine Item-ID begrenzen")
    private String item;

    @Option(
            names = "--profile",
            required = true,
            description = "Website-Profil als JSON; mehrfach möglich")
    private List<Path> paths;

    @Option(
            names = "--suffix",
            required = true,
            description = "Exakte verfügbare SimpleLogin-Endung, z. B. @meine-domain.de")
    private String suffix;

    @Option(
            names = "--mailbox",
            required = true,
            split = ",",
            description = "SimpleLogin-Mailbox-ID(s)")
    private List<Long> mailboxes;

    @Option(names = "--execute", description = "Alias und Zugangsdaten tatsächlich ändern")
    private boolean execute;

    @Option(names = "--headless", description = "Ohne Browserfenster (keine PAUSE-Schritte)")
    private boolean headless;

    @Override
    protected void execute() {
        runMigration(!execute);
    }

    @Override
    protected void preview() {
        runMigration(true);
    }

    private void runMigration(boolean planOnly) {
        var destination = new AliasDestination(suffix, mailboxes);
        var components = components();
        var profiles = paths.stream().map(components.profiles()::load).toList();
        profiles.forEach(profile -> profile.checkMode(headless));
        var pass = components.pass();
        MigrationPlan plan =
                new RotationPlanner(pass).plan(vault, item, profiles, destination.suffix());
        printPlan(plan);
        if (planOnly) {
            out().println(
                            "Plan: keine Aliase angelegt, keine Zugangsdaten geändert. Ausführung"
                                    + " ohne --dry-run und mit --execute.");
            describePreview(
                    "Geprüft: Profile, Profilzuordnung und doppelte Aliasziele anhand der gelesenen"
                        + " Proton-Einträge.",
                    "Kein SimpleLogin-Zugriff, kein Browserstart, keine Passwortgenerierung und"
                        + " keine lokalen Schreibzugriffe.",
                    "Alias-Verfügbarkeit, Website-Abläufe und bestehende Journalzustände wurden"
                        + " nicht geprüft.");
            return;
        }
        try (var store = components.openState()) {
            var service =
                    new RotationService(
                            pass,
                            components.aliases(store),
                            components.website(),
                            new Journal(store));
            for (var job : plan.jobs()) {
                var outcome =
                        service.rotate(
                                job.account(),
                                job.profile(),
                                destination.suffix(),
                                destination.mailboxes(),
                                headless);
                out().println(job.account().id() + "\t" + outcome);
            }
        }
    }

    private void printPlan(MigrationPlan plan) {
        var components = components();
        plan.skippedIds().forEach(id -> out().println(id + "\tÜBERSPRUNGEN: kein Website-Profil"));
        for (var job : plan.jobs()) {
            out().println(
                            job.account().id()
                                    + "\t"
                                    + components.display(job.profile().name())
                                    + "\t"
                                    + components.display(job.profile().aliasPrefix() + suffix));
        }
    }
}
