package de.jsc3r.pass2sec.migration.entity;

/** Contains only a state summary, never credentials from the recovery journal. */
public record RecoveryPlan(Stage stage, VaultUpdateState vaultState) {}
