package de.jsc3r.pass2sec.vault.entity;

import java.util.List;

public record Account(
        String vault,
        String id,
        String title,
        String username,
        String email,
        String password,
        List<String> urls) {
    public Account {
        urls = List.copyOf(urls);
    }

    @Override
    public String toString() {
        return "Account[redacted]";
    }
}
