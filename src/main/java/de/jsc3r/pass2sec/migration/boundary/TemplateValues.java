package de.jsc3r.pass2sec.migration.boundary;

import de.jsc3r.pass2sec.migration.entity.Rotation;

public final class TemplateValues {
    private TemplateValues() {}

    public static String resolve(String value, Rotation rotation) {
        if (value == null) return "";
        return switch (value) {
            case "${username}" -> rotation.original().username();
            case "${oldEmail}" -> rotation.original().email();
            case "${oldPassword}" -> rotation.original().password();
            case "${newEmail}" -> rotation.email();
            case "${newPassword}" -> rotation.password();
            default -> value;
        };
    }
}
