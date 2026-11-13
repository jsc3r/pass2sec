package de.jsc3r.pass2sec.application.boundary.cli;

import de.jsc3r.pass2sec.authentication.boundary.cli.AuthCommand;
import de.jsc3r.pass2sec.authentication.boundary.cli.KeygenCommand;
import de.jsc3r.pass2sec.migration.boundary.cli.RecoverCommand;
import de.jsc3r.pass2sec.migration.boundary.cli.RotateCommand;
import de.jsc3r.pass2sec.migration.boundary.cli.StatusCommand;
import de.jsc3r.pass2sec.vault.boundary.cli.AccountsCommand;
import de.jsc3r.pass2sec.vault.boundary.cli.VaultsCommand;

import io.quarkus.picocli.runtime.annotations.TopCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@TopCommand
@Command(
        name = "pass2sec",
        scope = ScopeType.INHERIT,
        version = "pass2sec 1.0",
        description = "Proton-Pass-Logins auf SimpleLogin-Aliase und neue Passwörter migrieren.",
        synopsisHeading = "Aufruf: ",
        synopsisSubcommandLabel = "[BEFEHL]",
        descriptionHeading = "%nZweck:%n",
        optionListHeading = "%nOptionen:%n",
        commandListHeading = "%nVerfügbare Befehle:%n",
        footerHeading = "%nBeispiele und nächste Schritte:%n",
        showDefaultValues = true,
        footer = {
            "Ohne Befehl startet eine interaktive Sitzung mit dem Prompt pass2sec>.",
            "Dort z. B. help, auth help oder auth proton login --dry-run eingeben.",
            "exit oder quit beendet die Sitzung; Strg+D ebenfalls. Keine Befehlshistorie.",
            "Optionen gelten nur für den aktuellen Befehl; Startoptionen sind Sitzungsvorgaben.",
            "Ein beim Start gesetztes --dry-run gilt für die gesamte Sitzung.",
            "",
            "Jeder Aufruf beginnt mit pass2sec (bzw. java -jar"
                    + " target/quarkus-app/quarkus-run.jar).",
            "",
            "Erste Einrichtung:",
            "  1. pass2sec auth proton login      Proton-Anmeldung im Browser abschließen.",
            "  2. pass2sec keygen                 Journal-Schlüssel erzeugen und sicher"
                    + " aufbewahren.",
            "  3. pass2sec auth simplelogin login SimpleLogin anmelden und API-Key verschlüsselt"
                    + " speichern.",
            "     Alternativ: pass2sec auth simplelogin set-key (vorhandenen Key verdeckt"
                    + " einlesen).",
            "",
            "Accounts migrieren:",
            "  4. pass2sec vaults                 Tresore und deren Share-IDs anzeigen.",
            "  5. pass2sec accounts --vault SHARE_ID",
            "  6. Website-Profil aus profiles/example.json anpassen; dies ist nur eine Vorlage.",
            "  7. Zunächst einen einzelnen Account planen:",
            "     pass2sec rotate --vault SHARE_ID --item ITEM_ID \\",
            "       --profile profiles/mein-dienst.json \\",
            "       --suffix @meine-domain.de --mailbox 123",
            "     Ohne --execute wird nur geplant; für echte Änderungen --execute ergänzen.",
            "",
            "Nach einem Abbruch:",
            "  pass2sec status --vault SHARE_ID --item ITEM_ID",
            "  pass2sec recover --vault SHARE_ID --item ITEM_ID \\",
            "    --profile profiles/mein-dienst.json",
            "  Zuerst den Zustand prüfen; offene Vorgänge nicht durch erneutes rotate umgehen.",
            "",
            "Hilfe überall auf der Befehlsebene: help, --help oder -h.",
            "  pass2sec help rotate",
            "  pass2sec auth help",
            "  pass2sec help auth simplelogin login",
            "  Auch während eigener Eingabeabfragen: help eingeben; danach dieselbe Eingabe"
                    + " fortsetzen.",
            "  Für den wörtlichen Eingabewert help: \\help verwenden. Strg+C bricht ab.",
            "  Proton-Login und Playwright Inspector haben ihre eigene Benutzerführung.",
            "",
            "Voraussetzungen: JDK 25, installierte pass-cli, Chromium für Website-Abläufe.",
            "API-Key und Journal liegen verschlüsselt in .pass2sec/; denselben Journal-Schlüssel"
                    + " behalten.",
            "PASS2SEC_JOURNAL_KEY vermeidet die Schlüsselabfrage; SIMPLELOGIN_API_KEY überschreibt"
                    + " den gespeicherten Key.",
            "Hilfe ist immer lokal: Sie liest keine Geheimnisse und führt keine Änderungen aus.",
            "--dry-run kann vor oder nach dem Befehl stehen und deaktiviert Änderungen, auch mit"
                    + " --execute.",
            "  pass2sec auth simplelogin login --dry-run",
            "  pass2sec rotate --dry-run ...",
            "  pass2sec recover --dry-run ...",
            "Listen und Migrationspläne können weiterhin lesend auf Proton zugreifen; Anmeldungen"
                    + " werden nur beschrieben.",
            "Weitere Einrichtung, Browserinstallation und Profilformat: README.md."
        },
        subcommands = {
            HelpCommand.class,
            VaultsCommand.class,
            AccountsCommand.class,
            RotateCommand.class,
            RecoverCommand.class,
            StatusCommand.class,
            KeygenCommand.class,
            AuthCommand.class
        })
public final class Pass2Sec implements Runnable {
    private final boolean interactive;

    public Pass2Sec() {
        this(true);
    }

    Pass2Sec(boolean interactive) {
        this.interactive = interactive;
    }

    // Inherited options avoid ResourceBundle lookup through Picocli's classloader,
    // which cannot see application resources in Quarkus dev mode.
    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            scope = ScopeType.INHERIT,
            description = "Diese Hilfe anzeigen und beenden.")
    private boolean helpRequested;

    @Option(
            names = {"-V", "--version"},
            versionHelp = true,
            scope = ScopeType.INHERIT,
            description = "Versionsinformationen anzeigen und beenden.")
    private boolean versionRequested;

    @Option(
            names = "--dry-run",
            scope = ScopeType.INHERIT,
            description =
                    "Nur prüfen und geplante Schritte anzeigen; Änderungen deaktivieren (hat"
                            + " Vorrang vor --execute)")
    private boolean dryRun;

    public boolean dryRun() {
        return dryRun;
    }

    @Option(
            names = "--pass-cli",
            defaultValue = "pass-cli",
            scope = ScopeType.INHERIT,
            description = "Pfad zur offiziellen Proton Pass CLI")
    private String binary;

    @Option(
            names = "--state-dir",
            defaultValue = ".pass2sec",
            scope = ScopeType.INHERIT,
            description = "Privates Verzeichnis für Journal und verschlüsselte Zugangsdaten (0700)")
    private Path stateDir;

    @Option(
            names = "--simplelogin-url",
            defaultValue = "https://app.simplelogin.io",
            scope = ScopeType.INHERIT,
            description = "HTTPS-Basisadresse von SimpleLogin")
    private URI simpleloginUrl;

    @Spec private picocli.CommandLine.Model.CommandSpec spec;

    public ApplicationComponents components() {
        return components(() -> spec.commandLine().usage(spec.commandLine().getOut()));
    }

    public ApplicationComponents components(Runnable showHelp) {
        return new ApplicationComponents(binary, stateDir, simpleloginUrl, showHelp);
    }

    @Override
    public void run() {
        if (!interactive) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }
        var defaults =
                new ArrayList<>(
                        List.of(
                                "--pass-cli",
                                binary,
                                "--state-dir",
                                stateDir.toString(),
                                "--simplelogin-url",
                                simpleloginUrl.toString()));
        if (dryRun) defaults.add("--dry-run");
        var parent = spec.commandLine();
        new InteractiveShell(
                        new ConsoleShellInput(),
                        parent.getOut(),
                        parent.getErr(),
                        () -> new picocli.CommandLine(new Pass2Sec(false), parent.getFactory()),
                        defaults)
                .run();
    }
}
