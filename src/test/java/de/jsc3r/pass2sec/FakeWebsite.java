package de.jsc3r.pass2sec;

import de.jsc3r.pass2sec.migration.control.port.Website;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.util.List;

final class FakeWebsite implements Website {
    private final List<String> events;
    boolean failVerify;
    boolean failChange;
    Runnable onVerify = () -> {};

    FakeWebsite(List<String> events) {
        this.events = events;
    }

    @Override
    public void change(Profile profile, Rotation rotation, boolean headless) {
        events.add("change");
        if (failChange) throw new Failure("simulated");
    }

    @Override
    public void verify(Profile profile, Rotation rotation, boolean headless) {
        events.add("verify");
        if (failVerify) throw new Failure("simulated");
        onVerify.run();
    }
}
