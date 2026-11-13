package de.jsc3r.pass2sec.application.boundary.cli;

import java.io.IOException;

@FunctionalInterface
public interface ShellInput {
    String readLine() throws IOException;
}
