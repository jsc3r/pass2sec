package de.jsc3r.pass2sec;

import de.jsc3r.pass2sec.alias.control.port.Aliases;
import de.jsc3r.pass2sec.alias.entity.AliasPlan;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.util.List;

final class FakeAliases implements Aliases {
    private final List<String> events;
    boolean fail;

    FakeAliases(List<String> events) {
        this.events = events;
    }

    @Override
    public AliasPlan plan(String host, String prefix, String suffix, List<Long> mailboxes) {
        events.add("plan");
        return new AliasPlan(host, prefix, suffix, "signed", mailboxes);
    }

    @Override
    public void create(AliasPlan plan) {
        events.add("alias");
        if (fail) throw new Failure("simulated");
    }
}
