package de.jsc3r.pass2sec.vault.control.port;

import de.jsc3r.pass2sec.vault.entity.Account;
import de.jsc3r.pass2sec.vault.entity.Summary;
import de.jsc3r.pass2sec.vault.entity.Vault;

import java.util.List;

/** Port for the vault and password operations required by migration. */
public interface Pass {
    List<Vault> vaults();

    List<Summary> items(String vault);

    Account read(String vault, String item);

    String generate(int length);

    void update(Account original, String email, String password, boolean updateUsername);
}
