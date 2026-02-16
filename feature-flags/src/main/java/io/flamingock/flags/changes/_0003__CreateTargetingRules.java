package io.flamingock.flags.changes;

import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

import java.sql.Connection;
import java.sql.Statement;

@TargetSystem(id = "postgres-flags")
@Change(id = "create-targeting-rules", author = "dev")
public class _0003__CreateTargetingRules {

    @Apply
    public void apply(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS \"pgcrypto\"");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS targeting_rules (
                        id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                        flag_name  VARCHAR(255) REFERENCES feature_flags(name),
                        attribute  VARCHAR(255) NOT NULL,
                        operator   VARCHAR(50) NOT NULL,
                        value      TEXT NOT NULL,
                        created_at TIMESTAMPTZ DEFAULT NOW()
                    )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_targeting_rules_flag_name ON targeting_rules(flag_name)");
        }
    }

    @Rollback
    public void rollback(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS targeting_rules");
        }
    }
}
