/*
 * Copyright 2023 Flamingock (https://www.flamingock.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flamingock.examples.inventory.changes;

import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;
import io.flamingock.examples.inventory.util.KafkaSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.flamingock.examples.inventory.TargetSystems.KAFKA_TARGET_SYSTEM;

@TargetSystem(id =KAFKA_TARGET_SYSTEM)
@Change(id = "cleanup-old-schema-version", author = "flamingock-team", transactional = false)
public class _0007__kafka_leanupOldSchemaVersion {

    private static final Logger logger = LoggerFactory.getLogger(_0007__kafka_leanupOldSchemaVersion.class);

    private static final String SUBJECT_NAME = "order-created-value";

    @Apply
    public void apply(KafkaSchemaManager schemaManager) throws Exception {
        logger.info("Marking old schema version for deprecation");

        // In production, you would typically:
        // 1. Wait for all consumers to upgrade
        // 2. Monitor consumer lag
        // 3. Then mark old versions as deprecated

        // For demo purposes, we'll just add metadata
        int latestVersion = schemaManager.getLatestSchemaVersion(SUBJECT_NAME);

        if (latestVersion > 1) {
            // Add deprecation metadata (in a real system)
            logger.info("Schema V1 marked for deprecation. Current latest version: V{}", latestVersion);
            logger.info("Note: Old schema versions remain available for backward compatibility");

            // Log migration statistics
            logger.info("Migration Statistics:");
            logger.info("  - Total schema versions: {}", latestVersion);
            logger.info("  - Active version: V{}", latestVersion);
            logger.info("  - Deprecated versions: V1");
            logger.info("  - Compatibility mode: BACKWARD");
        } else {
            logger.warn("No newer schema version found - skipping deprecation");
        }
    }

    @Rollback
    public void rollback(KafkaSchemaManager schemaManager) throws Exception {
        logger.info("Rolling back: Removing deprecation marker from old schema");

        // In a real scenario, this would remove the deprecation metadata
        // For demo purposes, we just log the action
        logger.info("Schema V1 deprecation marker removed - all versions now active");
    }
}