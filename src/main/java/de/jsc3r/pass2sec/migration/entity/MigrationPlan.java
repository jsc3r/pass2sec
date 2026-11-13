package de.jsc3r.pass2sec.migration.entity;

import java.util.List;

public record MigrationPlan(List<RotationJob> jobs, List<String> skippedIds) {
    public MigrationPlan {
        jobs = List.copyOf(jobs);
        skippedIds = List.copyOf(skippedIds);
    }
}
