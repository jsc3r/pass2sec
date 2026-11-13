package de.jsc3r.pass2sec.authentication.boundary.cli;

import de.jsc3r.pass2sec.application.boundary.cli.SafeCommand;

import picocli.CommandLine.Command;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Command(
        name = "keygen",
        description = "Journal-Schlüssel erzeugen; sicher aufbewahren",
        footer = {
            "Beispiel: pass2sec keygen",
            "Mit --dry-run: nur beschreiben; keinen Schlüssel erzeugen oder ausgeben.",
            "Gibt einen neuen zufälligen 256-Bit-Schlüssel in Base64 aus. Diesen separat sicher"
                    + " aufbewahren.",
            "Der Schlüssel wird nicht automatisch auf Platte gespeichert und ersetzt keinen"
                    + " bestehenden Schlüssel.",
            "Für bestehende Journal- und Zugangsdaten immer den bisherigen Schlüssel verwenden.",
            "Bei Bedarf verdeckt eingeben oder PASS2SEC_JOURNAL_KEY über eine Secret-Verwaltung"
                    + " setzen.",
            "Danach: pass2sec auth simplelogin login oder pass2sec auth simplelogin set-key.",
            "Alle Befehle: pass2sec help. keygen help zeigt nur Hilfe und erzeugt keinen Schlüssel."
        })
public final class KeygenCommand extends SafeCommand {
    @Override
    protected void preview() {
        describePreview(
                "Würde einen zufälligen 256-Bit-Journal-Schlüssel in Base64 erzeugen und ausgeben.",
                "Es wird kein Schlüssel erzeugt, ausgegeben oder gespeichert.");
    }

    @Override
    protected void execute() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        out().println(Base64.getEncoder().encodeToString(key));
        Arrays.fill(key, (byte) 0);
    }
}
