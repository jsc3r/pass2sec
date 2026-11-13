package de.jsc3r.pass2sec.vault.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "accounts",
        description = "Login-Einträge eines Tresors ohne Passwörter auflisten",
        footer = {
            "Beispiel: pass2sec accounts --vault SHARE_ID",
            "Mit --dry-run: dieselbe lesende Auflistung über die vorhandene Proton-Sitzung, als"
                + " Probelauf gekennzeichnet.",
            "Vorher: pass2sec auth proton login und pass2sec vaults.",
            "Zeigt nur aktive Login-Einträge mit Item-ID und Titel; keine Passwörter.",
            "Für eine einzelne Migration die Item-ID bei rotate mit --item auswählen.",
            "Danach: pass2sec help rotate für Website-Profil, Alias-Endung und Mailbox-ID.",
            "Alle Befehle: pass2sec help. accounts help funktioniert auch ohne --vault."
        })
public final class AccountsCommand extends SafeCommand {
    @Override
    protected void preview() {
        describePreview(
                "Nur lesend: aktive Login-Einträge über die bestehende Proton-CLI-Sitzung"
                    + " auflisten.");
        execute();
    }

    @Option(names = "--vault", required = true, description = "Share-ID des Tresors")
    private String vault;

    @Override
    protected void execute() {
        var components = components();
        for (var item : components.pass().items(vault)) {
            out().println(item.id() + "\t" + components.display(item.title()));
        }
    }
}
