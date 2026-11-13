package de.jsc3r.pass2sec.migration.control.port;

import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.profile.entity.Profile;

public interface Website {
    void change(Profile profile, Rotation rotation, boolean headless);

    void verify(Profile profile, Rotation rotation, boolean headless);
}
