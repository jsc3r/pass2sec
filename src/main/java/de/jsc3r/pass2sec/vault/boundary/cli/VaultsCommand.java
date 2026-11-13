package de.jsc3r.pass2sec.vault.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;

import picocli.CommandLine.Command;

@Command(
        name = "vaults",
        description = "Tresore auflisten (nur lesend)",
        footer = {
            "Beispiel: pass2sec vaults",
            "Mit --dry-run: dieselbe lesende Auflistung über die vorhandene Proton-Sitzung, als"
                + " Probelauf gekennzeichnet.",
            "Voraussetzung: Mit pass2sec auth proton login angemeldet sein und Leserechte"
                    + " besitzen.",
            "Zeigt Namen und Share-IDs der verfügbaren Tresore; es werden keine Passwörter"
                    + " ausgegeben.",
            "Die Share-ID (nicht die Vault-ID) für --vault verwenden.",
            "Danach: pass2sec accounts --vault SHARE_ID; Hilfe dazu: pass2sec help accounts.",
            "Alle Befehle und Einrichtungsreihenfolge: pass2sec help."
        })
public final class VaultsCommand extends SafeCommand {
    @Override
    protected void preview() {
        describePreview("Nur lesend: Tresore über die bestehende Proton-CLI-Sitzung auflisten.");
        execute();
    }

    @Override
    protected void execute() {
        var components = components();
        for (var vault : components.pass().vaults()) {
            out().println(vault.shareId() + "\t" + components.display(vault.name()));
        }
    }
}
