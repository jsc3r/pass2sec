package de.jsc3r.pass2sec.profile.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.nio.file.Path;

public final class ProfileLoader {
    private final ObjectMapper json;

    public ProfileLoader(ObjectMapper json) {
        this.json = json;
    }

    public Profile load(Path path) {
        try {
            Profile profile = json.readValue(path.toFile(), Profile.class);
            profile.validate();
            return profile;
        } catch (Failure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new Failure("Website-Profil nicht lesbar oder ungültig.");
        }
    }
}
