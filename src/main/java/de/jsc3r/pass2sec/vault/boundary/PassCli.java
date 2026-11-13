package de.jsc3r.pass2sec.vault.boundary;

import static de.jsc3r.pass2sec.shared.boundary.JsonFields.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.jsc3r.pass2sec.shared.boundary.NativeProcessRunner;
import de.jsc3r.pass2sec.shared.control.port.ProcessRunner;
import de.jsc3r.pass2sec.shared.entity.Failure;
import de.jsc3r.pass2sec.vault.control.port.Pass;
import de.jsc3r.pass2sec.vault.entity.Account;
import de.jsc3r.pass2sec.vault.entity.Summary;
import de.jsc3r.pass2sec.vault.entity.Vault;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Adapter to the official CLI; credentials never pass through a shell. */
public final class PassCli implements Pass {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(90);
    private final String binary;
    private final ObjectMapper json;
    private final ProcessRunner processes;

    public PassCli(String binary, ObjectMapper json) {
        this(binary, json, new NativeProcessRunner());
    }

    public PassCli(String binary, ObjectMapper json, ProcessRunner processes) {
        this.binary = binary;
        this.json = json;
        this.processes = processes;
    }

    public void login() {
        processes.interactive(command("login"), Duration.ofMinutes(15));
    }

    public void logout() {
        processes.interactive(command("logout"), COMMAND_TIMEOUT);
    }

    @Override
    public List<Vault> vaults() {
        var result = new ArrayList<Vault>();
        for (var vault : requiredArray(query("vault", "list", "--output", "json"), "vaults")) {
            result.add(new Vault(required(vault, "share_id"), required(vault, "name")));
        }
        return List.copyOf(result);
    }

    @Override
    public List<Summary> items(String vault) {
        var result = new ArrayList<Summary>();
        JsonNode response =
                query(
                        "item",
                        "list",
                        "--share-id",
                        vault,
                        "--filter-type",
                        "login",
                        "--filter-state",
                        "active",
                        "--output",
                        "json");
        for (var item : requiredArray(response, "items")) {
            result.add(new Summary(required(item, "id"), required(item, "title")));
        }
        return List.copyOf(result);
    }

    @Override
    public Account read(String vault, String id) {
        JsonNode response =
                query("item", "view", "--share-id", vault, "--item-id", id, "--output", "json");
        return new PassItemDecoder().decode(vault, id, response);
    }

    @Override
    public String generate(int length) {
        String password =
                required(
                        query(
                                "password",
                                "generate",
                                "random",
                                "--length",
                                String.valueOf(length),
                                "--numbers",
                                "true",
                                "--uppercase",
                                "true",
                                "--symbols",
                                "true",
                                "--output",
                                "json"),
                        "password");
        if (password.length() != length || password.contains("\n") || password.contains("\r")) {
            throw new Failure("Unerwartete Passwortausgabe von Proton.");
        }
        return password;
    }

    @Override
    public void update(Account original, String email, String password, boolean updateUsername) {
        List<String> args =
                command(
                        "item",
                        "update",
                        "--share-id",
                        original.vault(),
                        "--item-id",
                        original.id(),
                        "--field",
                        "email=" + email,
                        "--field",
                        "password=" + password);
        if (updateUsername) args.addAll(List.of("--field", "username=" + email));
        processes.discard(args, COMMAND_TIMEOUT);
    }

    private JsonNode query(String... args) {
        String output = processes.capture(command(args), COMMAND_TIMEOUT);
        try {
            JsonNode response = json.readTree(output);
            if (response == null) throw new Failure("Leere Ausgabe der Proton-CLI.");
            return response;
        } catch (Failure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new Failure("Unerwartetes JSON-Format der Proton-CLI.");
        }
    }

    private List<String> command(String... args) {
        var command = new ArrayList<String>();
        command.add(binary);
        command.addAll(List.of(args));
        return command;
    }
}
