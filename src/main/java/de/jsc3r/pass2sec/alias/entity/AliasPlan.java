package de.jsc3r.pass2sec.alias.entity;

import java.util.List;

public record AliasPlan(
        String host, String prefix, String suffix, String signedSuffix, List<Long> mailboxes) {
    public AliasPlan {
        mailboxes = List.copyOf(mailboxes);
    }

    public String email() {
        return prefix + suffix;
    }

    @Override
    public String toString() {
        return "AliasPlan[redacted]";
    }
}
