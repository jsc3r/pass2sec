package de.jsc3r.pass2sec.authentication.entity;

public record ApiCredentials(String server, String apiKey) {
    @Override
    public String toString() {
        return "ApiCredentials[redacted]";
    }
}
