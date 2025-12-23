package io.flamingock.examples.inventory;

import com.mongodb.client.MongoClient;
import io.flamingock.community.mongodb.sync.driver.MongoDBSyncAuditStore;
import io.flamingock.examples.inventory.util.MongoDBUtil;
import io.flamingock.internal.core.store.CommunityAuditStore;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import io.flamingock.targetystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlamingockConfig {

    @Bean
    public MongoClient mongoClient() {
        return MongoDBUtil.getMongoClient("mongodb://localhost:27017/");
    }

    @Bean
    public MongoDBSyncTargetSystem mongoDBSyncTargetSystem(MongoClient mongoClient) {
        return new MongoDBSyncTargetSystem(TargetSystems.MONGODB_TARGET_SYSTEM, mongoClient, TargetSystems.DATABASE_NAME);
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
