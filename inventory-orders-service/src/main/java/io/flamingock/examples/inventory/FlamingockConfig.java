package io.flamingock.examples.inventory;

import io.flamingock.community.mongodb.sync.driver.MongoDBSyncAuditStore;
import io.flamingock.internal.core.store.CommunityAuditStore;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import io.flamingock.targetystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlamingockConfig {

    @Bean
    public MongoDBSyncTargetSystem mongoDBSyncTargetSystem() {
        return TargetSystems.mongoDBSyncTargetSystem();
    }

    @Bean
    public NonTransactionalTargetSystem kafkaTargetSystem() throws Exception {
        return TargetSystems.kafkaTargetSystem();
    }

    @Bean
    public NonTransactionalTargetSystem toggleTargetSystem() {
        return TargetSystems.toggleTargetSystem();
    }


    //This could return any of the available community audit stores
    @Bean
    public CommunityAuditStore auditStore(MongoDBSyncTargetSystem mongoDBSyncTargetSystem) {
        return MongoDBSyncAuditStore.from(mongoDBSyncTargetSystem);
    }
}
