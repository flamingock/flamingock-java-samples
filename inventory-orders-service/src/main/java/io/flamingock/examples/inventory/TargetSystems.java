package io.flamingock.examples.inventory;

import com.mongodb.client.MongoClient;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.flamingock.examples.inventory.util.KafkaSchemaManager;
import io.flamingock.examples.inventory.util.LaunchDarklyClient;
import io.flamingock.examples.inventory.util.MongoDBUtil;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import io.flamingock.targetsystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Collections;
import java.util.Properties;

public final class TargetSystems {
    public static final String MONGODB_TARGET_SYSTEM = "mongodb-inventory";
    public static final String KAFKA_TARGET_SYSTEM = "kafka-inventory";
    public static final String FEATURE_FLAG_TARGET_SYSTEM = "toggle-inventory";


    public static final String DATABASE_NAME = "inventory";
    public static final String CONFIG_FILE_PATH = "config/application.yml";

    private TargetSystems() {}

    public static NonTransactionalTargetSystem toggleTargetSystem() {
        // Create LaunchDarkly Management API client for demonstration
        // In demo mode, this uses a dummy token and will log intended operations
        LaunchDarklyClient launchDarklyClient = new LaunchDarklyClient(
            "demo-token", // In real usage, this would be your LaunchDarkly API token
            "inventory-service",
            "production"
        );

        return new NonTransactionalTargetSystem(FEATURE_FLAG_TARGET_SYSTEM).addDependency(launchDarklyClient);
    }

    public static MongoDBSyncTargetSystem mongoDBSyncTargetSystem() {
        MongoClient mongoClient = MongoDBUtil.getMongoClient("mongodb://localhost:27017/");
        return new MongoDBSyncTargetSystem(MONGODB_TARGET_SYSTEM, mongoClient, DATABASE_NAME);
    }

    public static NonTransactionalTargetSystem kafkaTargetSystem() throws Exception {
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(
                Collections.singletonList("http://localhost:8081"),
                100
        );

        // Kafka Admin client for topic management
        Properties kafkaProps = new Properties();
        kafkaProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        AdminClient kafkaAdminClient = AdminClient.create(kafkaProps);

        // Kafka schema manager
        KafkaSchemaManager schemaManager = new KafkaSchemaManager(schemaRegistryClient, kafkaAdminClient);
        //We simulate the topic is already created
        schemaManager.createTopicIfNotExists("order-created", 3, (short) 1);
        return new NonTransactionalTargetSystem(KAFKA_TARGET_SYSTEM).addDependency(schemaManager);
    }

}
