package de.jsc3r.pass2sec.authentication.control.port;

public interface SecretInput {
    String readLine(String prompt);

    char[] readSecret(String prompt);
}
