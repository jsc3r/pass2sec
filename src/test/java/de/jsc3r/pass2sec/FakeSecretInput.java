package de.jsc3r.pass2sec;

import de.jsc3r.pass2sec.authentication.control.port.SecretInput;

import java.util.ArrayDeque;
import java.util.Deque;

final class FakeSecretInput implements SecretInput {
    final Deque<String> lines = new ArrayDeque<>();
    final Deque<char[]> secrets = new ArrayDeque<>();

    @Override
    public String readLine(String prompt) {
        return lines.removeFirst();
    }

    @Override
    public char[] readSecret(String prompt) {
        return secrets.removeFirst();
    }
}
