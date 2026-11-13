package de.jsc3r.pass2sec.alias.control.port;

import de.jsc3r.pass2sec.alias.entity.AliasPlan;

import java.util.List;

public interface Aliases {
    AliasPlan plan(String host, String prefix, String suffix, List<Long> mailboxes);

    void create(AliasPlan plan);
}
