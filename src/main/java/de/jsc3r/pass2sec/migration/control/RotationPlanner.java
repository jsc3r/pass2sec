package de.jsc3r.pass2sec.migration.control;

import de.jsc3r.pass2sec.migration.entity.MigrationPlan;
import de.jsc3r.pass2sec.migration.entity.RotationJob;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;
import de.jsc3r.pass2sec.vault.entity.Account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class RotationPlanner {
    private final Pass pass;

    public RotationPlanner(Pass pass) {
        this.pass = pass;
    }

    public MigrationPlan plan(String vault, String itemId, List<Profile> profiles, String suffix) {
        var selected =
                pass.items(vault).stream()
                        .filter(item -> itemId == null || itemId.equals(item.id()))
                        .toList();
        if (selected.isEmpty())
            throw new Failure("Keine passenden aktiven Login-Einträge gefunden.");
        var jobs = new ArrayList<RotationJob>();
        var skipped = new ArrayList<String>();
        var addresses = new HashSet<String>();
        for (var item : selected) {
            Account account = pass.read(vault, item.id());
            var matches = profiles.stream().filter(profile -> profile.matches(account)).toList();
            if (matches.isEmpty()) {
                skipped.add(item.id());
                continue;
            }
            if (matches.size() != 1)
                throw new Failure(
                        "Mehrere Website-Profile passen zu einem Eintrag; Auswahl eingrenzen.");
            Profile profile = matches.getFirst();
            if (!addresses.add(profile.aliasPrefix() + suffix)) {
                throw new Failure(
                        "Mehrere Accounts würden denselben Alias erhalten; getrennte"
                                + " Profile/Alias-Präfixe verwenden.");
            }
            jobs.add(new RotationJob(account, profile));
        }
        if (jobs.isEmpty()) throw new Failure("Keine unterstützten Accounts in der Auswahl.");
        return new MigrationPlan(jobs, skipped);
    }
}
