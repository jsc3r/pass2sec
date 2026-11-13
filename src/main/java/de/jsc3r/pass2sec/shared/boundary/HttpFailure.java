package de.jsc3r.pass2sec.shared.boundary;

import de.jsc3r.pass2sec.shared.entity.Failure;

/** Contains only a status code, never a remote response or request body. */
public final class HttpFailure extends Failure {
    private final int status;

    public HttpFailure(int status) {
        super(
                "SimpleLogin-Anfrage fehlgeschlagen (HTTP "
                        + status
                        + "); keine automatische Wiederholung.");
        this.status = status;
    }

    public int status() {
        return status;
    }
}
