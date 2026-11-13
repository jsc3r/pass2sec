package de.jsc3r.pass2sec.migration.entity;

import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.vault.entity.Account;

public record RotationJob(Account account, Profile profile) {
    @Override
    public String toString() {
        return "RotationJob[redacted]";
    }
}
