package de.jsc3r.pass2sec.shared.entity;

/** Only these deliberately secret-free messages may reach the terminal. */
public class Failure extends RuntimeException {
    public Failure(String message) {
        super(message);
    }
}
