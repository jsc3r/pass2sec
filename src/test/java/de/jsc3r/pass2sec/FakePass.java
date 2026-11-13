package de.jsc3r.pass2sec;

import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;
import de.jsc3r.pass2sec.vault.entity.Account;
import de.jsc3r.pass2sec.vault.entity.Summary;
import de.jsc3r.pass2sec.vault.entity.Vault;

import java.util.List;

final class FakePass implements Pass {
    private final List<String> events;
    Account current = RotationServiceTest.OLD;
    boolean failUpdate;
    boolean loseAcknowledgement;

    FakePass(List<String> events) {
        this.events = events;
    }

    @Override
    public List<Vault> vaults() {
        return List.of(new Vault("vault", "Personal"));
    }

    @Override
    public List<Summary> items(String vault) {
        return List.of(new Summary("item", "Example"));
    }

    @Override
    public Account read(String vault, String item) {
        events.add("read");
        return current;
    }

    @Override
    public String generate(int length) {
        events.add("generate");
        return "new-secret";
    }

    @Override
    public void update(Account account, String email, String password, boolean updateUsername) {
        events.add("update");
        if (failUpdate) throw new Failure("simulated");
        current =
                new Account(
                        account.vault(),
                        account.id(),
                        account.title(),
                        updateUsername ? email : account.username(),
                        email,
                        password,
                        account.urls());
        if (loseAcknowledgement) throw new Failure("simulated lost acknowledgement");
    }
}
