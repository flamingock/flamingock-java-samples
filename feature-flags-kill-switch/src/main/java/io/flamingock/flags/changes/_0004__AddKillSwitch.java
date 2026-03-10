package io.flamingock.flags.changes;

import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

import java.sql.Connection;
import java.sql.Statement;

@TargetSystem(id = "postgres-flags")
@Change(id = "add-kill-switch", author = "dev")
public class _0004__AddKillSwitch {

    @Apply
    public void apply(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "ALTER TABLE feature_flags ADD COLUMN IF NOT EXISTS force_disabled BOOLEAN NOT NULL DEFAULT FALSE"
            );
        }
    }

    @Rollback
    public void rollback(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "ALTER TABLE feature_flags DROP COLUMN IF EXISTS force_disabled"
            );
        }
    }
}
