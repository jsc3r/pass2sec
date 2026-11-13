package de.jsc3r.pass2sec.migration.control;

import de.jsc3r.pass2sec.migration.control.port.RotationRepository;
import de.jsc3r.pass2sec.migration.control.port.Website;
import de.jsc3r.pass2sec.migration.entity.RecoveryPlan;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.RotationOutcome;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.migration.entity.VaultUpdateState;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;

/** Has no alias-creation dependency and never calls the website mutation flow. */
public final class RecoveryService {
    private final Website website;
    private final RotationRepository journal;
    private final RotationCommitter committer;

    public RecoveryService(Pass pass, Website website, RotationRepository journal) {
        this.website = website;
        this.journal = journal;
        this.committer = new RotationCommitter(pass, journal);
    }

    public RotationOutcome recover(String vault, String item, Profile profile, boolean headless) {
        Rotation rotation = load(vault, item, profile, headless);
        if (rotation.stage() == Stage.COMMITTED) return RotationOutcome.ALREADY_COMMITTED;
        if (rotation.stage() == Stage.PREPARED || rotation.stage() == Stage.ALIAS_READY) {
            throw new Failure(
                    "Abbruch vor Website-Start: Alias im SimpleLogin-Konto prüfen. Journal"
                            + " behalten; keine automatische Wiederholung.");
        }
        website.verify(profile, rotation, headless);
        Rotation verified = rotation.at(Stage.WEBSITE_VERIFIED);
        journal.save(verified);
        committer.commit(verified);
        return RotationOutcome.COMMITTED;
    }

    public RecoveryPlan preview(String vault, String item, Profile profile, boolean headless) {
        Rotation rotation = load(vault, item, profile, headless);
        if (rotation.stage() == Stage.PREPARED
                || rotation.stage() == Stage.ALIAS_READY
                || rotation.stage() == Stage.COMMITTED) {
            return new RecoveryPlan(rotation.stage(), VaultUpdateState.NOT_CHECKED);
        }
        return new RecoveryPlan(rotation.stage(), committer.inspect(rotation));
    }

    private Rotation load(String vault, String item, Profile profile, boolean headless) {
        profile.validate();
        profile.checkMode(headless);
        Rotation rotation =
                journal.read(vault, item)
                        .orElseThrow(() -> new Failure("Kein Wiederherstellungsvorgang gefunden."));
        validateProfile(rotation, profile);
        return rotation;
    }

    private void validateProfile(Rotation rotation, Profile profile) {
        if (!rotation.profile().equals(profile.name())
                || !profile.matches(rotation.original())
                || rotation.updateUsername() != profile.updateUsername()) {
            throw new Failure("Wiederherstellung benötigt das passende Website-Profil.");
        }
    }
}
