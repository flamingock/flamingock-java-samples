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

import static io.flamingock.examples.inventory.TargetSystems.KAFKA_TARGET_SYSTEM;

@TargetSystem(id =KAFKA_TARGET_SYSTEM)
@Change(id = "update-order-created-schema", author = "flamingock-team", transactional = false)
public class _0002__kafka_updateOrderCreatedSchema {

    private static final String SUBJECT_NAME = "order-created-value";

    @Apply
    public void apply(KafkaSchemaManager schemaManager) throws Exception {
        // Ensure topic exists
        schemaManager.createTopicIfNotExists("order-created", 3, (short) 1);

        // Register V2 schema (assumes V1 already exists as baseline)
        schemaManager.registerSchema(SUBJECT_NAME, SCHEMA_V2_WITH_DISCOUNT);
    }

    @Rollback
    public void rollback(KafkaSchemaManager schemaManager) {
//        logger.info("Rolling back schema evolution");
//        logger.info("In a production system, this would revert producers to use schema V1");
//        logger.info("Note: Both schema versions remain in registry for backward compatibility");
    }

    // Schema V2 - Adds discount field (backward compatible with V1)
    private static final String SCHEMA_V2_WITH_DISCOUNT = """
        {
          "type": "record",
          "name": "OrderCreated",
          "namespace": "io.flamingock.examples.inventory.events",
          "fields": [
            {"name": "orderId", "type": "string"},
            {"name": "customerId", "type": "string"},
            {"name": "total", "type": "double"},
            {"name": "status", "type": "string"},
            {"name": "createdAt", "type": "string"},
            {"name": "discountCode", "type": ["null", "string"], "default": null}
          ]
        }
        """;
}