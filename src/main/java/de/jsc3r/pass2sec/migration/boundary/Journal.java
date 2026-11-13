package de.jsc3r.pass2sec.migration.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.migration.control.port.RotationRepository;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.shared.boundary.EncryptedFileStore;
import de.jsc3r.pass2sec.shared.entity.Failure;

import java.nio.file.Path;
import java.util.Optional;

/** Repository adapter; existing filenames and ciphertext remain compatible. */
public final class Journal implements RotationRepository, AutoCloseable {
    private final EncryptedFileStore store;
    private final boolean ownsStore;

    public Journal(Path directory, String key, ObjectMapper json) {
        this.store = new EncryptedFileStore(directory, key, json);
        this.ownsStore = true;
    }

    public Journal(EncryptedFileStore store) {
        this.store = store;
        this.ownsStore = false;
    }

    @Override
    public Optional<Rotation> read(String vault, String item) {
        var result = store.read(fileName(vault, item), Rotation.class);
        result.ifPresent(
                rotation -> {
                    if (!vault.equals(rotation.original().vault())
                            || !item.equals(rotation.original().id())) {
                        throw new Failure("Journal enthält einen anderen Eintrag als angefordert.");
                    }
                });
        return result;
    }

    @Override
    public void save(Rotation rotation) {
        store.save(fileName(rotation.original().vault(), rotation.original().id()), rotation);
    }

    private String fileName(String vault, String item) {
        return EncryptedFileStore.fileName(vault + "\0" + item);
    }

    @Override
    public void close() {
        if (ownsStore) store.close();
    }
}
