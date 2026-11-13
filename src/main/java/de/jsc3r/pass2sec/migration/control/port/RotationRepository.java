package de.jsc3r.pass2sec.migration.control.port;

import de.jsc3r.pass2sec.migration.entity.Rotation;

import java.util.Optional;

public interface RotationRepository {
    Optional<Rotation> read(String vault, String item);

    void save(Rotation rotation);
}
