package de.jsc3r.pass2sec.profile.entity;

import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.entity.Account;

import java.net.URI;
import java.util.*;

/** Profiles are trusted executable configuration, not downloaded website content. */
public record Profile(
        String name,
        Set<String> hosts,
        String aliasPrefix,
        boolean updateUsername,
        int passwordLength,
        int timeoutSeconds,
        List<Step> change,
        List<Step> verify) {
    public void validate() {
        if (name == null
                || name.isBlank()
                || hosts == null
                || hosts.isEmpty()
                || hosts.stream().anyMatch(h -> h == null || !h.matches("[a-z0-9.-]+"))
                || aliasPrefix == null
                || !aliasPrefix.matches("[a-z0-9][a-z0-9._-]{0,63}")
                || passwordLength < 16
                || passwordLength > 128
                || timeoutSeconds < 1
                || timeoutSeconds > 600) throw new Failure("Ungültige Website-Profilparameter.");
        validateSteps(change);
        validateSteps(verify);
        // Fresh login must exercise the new password and assert the account's new email.
        if (verify.stream()
                        .noneMatch(
                                s ->
                                        s.action() == Action.FILL
                                                && "${newPassword}".equals(s.value()))
                || verify.stream()
                        .noneMatch(
                                s ->
                                        s.action() == Action.FILL
                                                && ("${newEmail}".equals(s.value())
                                                        || "${username}".equals(s.value())))
                || verify.stream()
                        .noneMatch(
                                s ->
                                        s.action() == Action.ASSERT_TEXT
                                                && "${newEmail}".equals(s.value()))
                || verify.stream().noneMatch(s -> s.action() == Action.CLICK))
            throw new Failure(
                    "Verifikation benötigt neuen Login und Textprüfung der neuen E-Mail-Adresse.");
        int passwordStep = -1, submitStep = -1, emailAssertion = -1;
        for (int i = 0; i < verify.size(); i++) {
            var s = verify.get(i);
            if ("${oldPassword}".equals(s.value()) || "${oldEmail}".equals(s.value()))
                throw new Failure("Verifikation darf keine alten Zugangsdaten verwenden.");
            if (s.action() == Action.FILL && "${newPassword}".equals(s.value())) passwordStep = i;
            if (s.action() == Action.CLICK && passwordStep >= 0) submitStep = i;
            if (s.action() == Action.ASSERT_TEXT && "${newEmail}".equals(s.value()))
                emailAssertion = i;
        }
        if (passwordStep < 0
                || submitStep <= passwordStep
                || emailAssertion <= submitStep
                || (updateUsername
                        && verify.stream()
                                .noneMatch(
                                        s ->
                                                s.action() == Action.FILL
                                                        && "${newEmail}".equals(s.value()))))
            throw new Failure(
                    "Verifikation muss erst neue Zugangsdaten absenden und danach die E-Mail"
                            + " prüfen.");
    }

    private void validateSteps(List<Step> steps) {
        if (steps == null || steps.isEmpty() || steps.getFirst().action() != Action.NAVIGATE)
            throw new Failure("Jeder Browser-Ablauf muss mit NAVIGATE beginnen.");
        for (var step : steps) {
            if (step.action() == null || step.target() == null || step.target().isBlank())
                throw new Failure("Ungültiger Browser-Schritt.");
            if (step.action() == Action.NAVIGATE) checkUrl(step.target());
            if ((step.action() == Action.FILL || step.action() == Action.ASSERT_TEXT)
                    && step.value() == null) throw new Failure("Browser-Schritt ohne Wert.");
            if (step.value() != null
                    && step.value().contains("${")
                    && !Set.of(
                                    "${username}",
                                    "${oldEmail}",
                                    "${oldPassword}",
                                    "${newEmail}",
                                    "${newPassword}")
                            .contains(step.value()))
                throw new Failure("Unbekannte Variable im Website-Profil.");
        }
    }

    public void checkUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme())
                    || !hosts.contains(uri.getHost())
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443))
                throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new Failure("Website-URL liegt außerhalb der erlaubten HTTPS-Hosts.");
        }
    }

    public boolean matches(Account account) {
        return account.urls().stream()
                .anyMatch(
                        url -> {
                            try {
                                checkUrl(url);
                                return true;
                            } catch (Failure e) {
                                return false;
                            }
                        });
    }

    public void checkMode(boolean headless) {
        if (headless
                && java.util.stream.Stream.concat(change.stream(), verify.stream())
                        .anyMatch(s -> s.action() == Action.PAUSE))
            throw new Failure(
                    "Dieses Profil benötigt einen sichtbaren Browser für manuelle Bestätigungen.");
    }
}
