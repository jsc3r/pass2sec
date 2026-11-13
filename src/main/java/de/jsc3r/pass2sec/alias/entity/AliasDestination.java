package de.jsc3r.pass2sec.alias.entity;

import de.jsc3r.pass2sec.shared.entity.Failure;

import java.util.List;

/** Local validation shared by migration planning and actual alias creation. */
public record AliasDestination(String suffix, List<Long> mailboxes) {
    public AliasDestination {
        if (suffix == null
                || !suffix.matches("[^@\\s]*@[a-zA-Z0-9.-]+")
                || mailboxes == null
                || mailboxes.isEmpty()
                || mailboxes.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new Failure("Alias-Endung oder Mailbox-IDs ungültig.");
        }
        mailboxes = List.copyOf(mailboxes);
    }
}
