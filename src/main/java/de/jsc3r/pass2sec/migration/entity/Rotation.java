package de.jsc3r.pass2sec.migration.entity;

import de.jsc3r.pass2sec.vault.entity.Account;

public record Rotation(
        Account original,
        String profile,
        String email,
        String password,
        boolean updateUsername,
        Stage stage) {
    public Rotation at(Stage next) {
        return new Rotation(original, profile, email, password, updateUsername, next);
    }

    @Override
    public String toString() {
        return "Rotation[" + stage + ", redacted]";
    }
}
