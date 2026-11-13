package de.jsc3r.pass2sec.migration.control;

import de.jsc3r.pass2sec.migration.control.port.RotationRepository;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.migration.entity.Stage;
import de.jsc3r.pass2sec.migration.entity.VaultUpdateState;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;
import de.jsc3r.pass2sec.vault.entity.Account;

/** Shared commit protocol for initial migration and recovery. */
public final class RotationCommitter {
    private final Pass pass;
    private final RotationRepository journal;

    public RotationCommitter(Pass pass, RotationRepository journal) {
        this.pass = pass;
        this.journal = journal;
    }

    public void commit(Rotation rotation) {
        if (rotation.stage() != Stage.WEBSITE_VERIFIED)
            throw new Failure("Tresor-Update ohne Website-Verifikation verweigert.");
        Account original = rotation.original();
        VaultUpdateState state = inspect(rotation);
        if (state != VaultUpdateState.UPDATED) {
            if (state != VaultUpdateState.ORIGINAL) {
                throw new Failure(
                        "Tresoreintrag wurde zwischenzeitlich verändert; kein Überschreiben."
                                + " Journal behalten.");
            }
            pass.update(original, rotation.email(), rotation.password(), rotation.updateUsername());
            if (inspect(rotation) != VaultUpdateState.UPDATED) {
                throw new Failure(
                        "Tresor-Update beim Nachlesen nicht bestätigt; Journal behalten, recover"
                                + " verwenden.");
            }
        }
        journal.save(rotation.at(Stage.COMMITTED));
    }

    /** Read-only comparison shared by recovery preview and the commit protocol. */
    public VaultUpdateState inspect(Rotation rotation) {
        Account original = rotation.original();
        Account current = pass.read(original.vault(), original.id());
        String username = rotation.updateUsername() ? rotation.email() : original.username();
        if (sameLogin(current, rotation.email(), rotation.password(), username))
            return VaultUpdateState.UPDATED;
        if (sameLogin(current, original.email(), original.password(), original.username()))
            return VaultUpdateState.ORIGINAL;
        return VaultUpdateState.CONFLICT;
    }

    private boolean sameLogin(Account account, String email, String password, String username) {
        return account.email().equals(email)
                && account.password().equals(password)
                && account.username().equals(username);
    }
}
