package io.flamingock.flags.changes;

import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

import java.sql.Connection;
import java.sql.Statement;

@TargetSystem(id = "postgres-flags")
@Change(id = "create-flag-change-log", author = "dev")
public class _0005__CreateFlagChangeLog {

    @Apply
    public void apply(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS flag_change_log (
                        id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                        flag_name   VARCHAR(255) REFERENCES feature_flags(name),
                        changed_by  VARCHAR(255),
                        action      VARCHAR(50) NOT NULL,
                        detail      TEXT,
                        changed_at  TIMESTAMPTZ DEFAULT NOW()
                    )
                    """);
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_flag_change_log_flag_name ON flag_change_log(flag_name)"
            );
        }
    }

    @Rollback
    public void rollback(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS flag_change_log");
        }
    }
}
