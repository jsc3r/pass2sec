package de.jsc3r.pass2sec.migration.control;

import de.jsc3r.pass2sec.alias.control.port.Aliases;
import de.jsc3r.pass2sec.alias.entity.AliasPlan;
import de.jsc3r.pass2sec.migration.control.port.RotationRepository;
import de.jsc3r.pass2sec.migration.control.port.Website;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.RotationOutcome;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;
import de.jsc3r.pass2sec.vault.entity.Account;

import java.util.List;

/** Coordinates mutations; uncertain external actions always follow a durable state transition. */
public final class RotationService {
    private final Pass pass;
    private final Aliases aliases;
    private final Website website;
    private final RotationRepository journal;
    private final RotationCommitter committer;

    public RotationService(
            Pass pass, Aliases aliases, Website website, RotationRepository journal) {
        this.pass = pass;
        this.aliases = aliases;
        this.website = website;
        this.journal = journal;
        this.committer = new RotationCommitter(pass, journal);
    }

    public RotationOutcome rotate(
            Account account,
            Profile profile,
            String suffix,
            List<Long> mailboxes,
            boolean headless) {
        validateTarget(account, profile, headless);
        if (alreadyCommitted(account)) return RotationOutcome.ALREADY_COMMITTED;
        String host = profile.hosts().stream().sorted().findFirst().orElseThrow();
        AliasPlan plan = aliases.plan(host, profile.aliasPrefix(), suffix, mailboxes);
        Rotation rotation = prepare(account, profile, plan);
        aliases.create(plan);
        rotation = transition(rotation, Stage.ALIAS_READY);
        rotation = transition(rotation, Stage.WEBSITE_STARTED);
        website.change(profile, rotation, headless);
        website.verify(profile, rotation, headless);
        rotation = transition(rotation, Stage.WEBSITE_VERIFIED);
        committer.commit(rotation);
        return RotationOutcome.COMMITTED;
    }

    private void validateTarget(Account account, Profile profile, boolean headless) {
        profile.validate();
        profile.checkMode(headless);
        if (!profile.matches(account))
            throw new Failure("Website-Profil passt zu keiner URL des Tresoreintrags.");
    }

    private boolean alreadyCommitted(Account account) {
        var previous = journal.read(account.vault(), account.id());
        if (previous.isEmpty()) return false;
        if (previous.get().stage() == Stage.COMMITTED) return true;
        throw new Failure(
                "Offener Vorgang vorhanden; recover verwenden. Kein neuer Alias und kein neuer"
                        + " Passwortwechsel.");
    }

    private Rotation prepare(Account account, Profile profile, AliasPlan plan) {
        var rotation =
                new Rotation(
                        account,
                        profile.name(),
                        plan.email(),
                        pass.generate(profile.passwordLength()),
                        profile.updateUsername(),
                        Stage.PREPARED);
        journal.save(rotation);
        return rotation;
    }

    private Rotation transition(Rotation rotation, Stage stage) {
        Rotation next = rotation.at(stage);
        journal.save(next);
        return next;
    }
}
