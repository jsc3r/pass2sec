package de.jsc3r.pass2sec.shared.control.port;

import java.time.Duration;
import java.util.List;

public interface ProcessRunner {
    String capture(List<String> command, Duration timeout);

    void discard(List<String> command, Duration timeout);

    void interactive(List<String> command, Duration timeout);
}
