package io.flamingock.flags.config;

import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import io.flamingock.internal.core.external.store.CommunityAuditStore;
import io.flamingock.store.sql.SqlAuditStore;
import io.flamingock.targetsystem.sql.SqlTargetSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableFlamingock(
        stages = @Stage(location = "io.flamingock.flags.changes")
)
public class FlamingockConfig {

    @Bean
    public SqlTargetSystem sqlTargetSystem(DataSource dataSource) {
        return new SqlTargetSystem("postgres-flags", dataSource);
    }

    @Bean
    public CommunityAuditStore auditStore(SqlTargetSystem sqlTargetSystem) {
        return SqlAuditStore.from(sqlTargetSystem);
    }
}
