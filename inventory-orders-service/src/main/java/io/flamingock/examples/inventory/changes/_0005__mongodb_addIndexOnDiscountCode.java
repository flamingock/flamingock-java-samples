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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import static io.flamingock.examples.inventory.TargetSystems.MONGODB_TARGET_SYSTEM;

@TargetSystem(id = MONGODB_TARGET_SYSTEM)
@Change(id = "add-index-on-discount-code", author = "flamingock-team", transactional = false)

public class _0005__mongodb_addIndexOnDiscountCode {

    private static final Logger logger = LoggerFactory.getLogger(_0005__mongodb_addIndexOnDiscountCode.class);

    private static final String INDEX_NAME = "discountCode_1";
    private static final String ORDERS_COLLECTION = "orders";

    @Apply
    public void apply(MongoTemplate mongoTemplate) {
        logger.info("Creating index on discountCode field for efficient reporting queries");

        MongoCollection<Document> orders = mongoTemplate.getCollection(ORDERS_COLLECTION);

        // Check if index already exists (idempotent operation)
        boolean indexExists = orders.listIndexes()
                .into(new java.util.ArrayList<>())
                .stream()
                .anyMatch(index -> INDEX_NAME.equals(index.getString("name")));

        if (indexExists) {
            logger.info("Index '{}' already exists on orders collection - skipping creation", INDEX_NAME);
            return;
        }

        // Create ascending index on discountCode field
        IndexOptions indexOptions = new IndexOptions().name(INDEX_NAME);
        orders.createIndex(Indexes.ascending("discountCode"), indexOptions);

        logger.info("✅ Successfully created index '{}' on discountCode field in orders collection", INDEX_NAME);
        logger.info("Sales team can now run efficient reporting queries on discount usage");
    }

    @Rollback
    public void rollback(MongoTemplate mongoTemplate) {
        logger.info("Rolling back: Dropping index on discountCode field");

        MongoCollection<Document> orders = mongoTemplate.getCollection(ORDERS_COLLECTION);

        try {
            // Check if index exists before attempting to drop it
            boolean indexExists = orders.listIndexes()
                    .into(new java.util.ArrayList<>())
                    .stream()
                    .anyMatch(index -> INDEX_NAME.equals(index.getString("name")));

            if (!indexExists) {
                logger.info("Index '{}' does not exist - nothing to roll back", INDEX_NAME);
                return;
            }

            orders.dropIndex(INDEX_NAME);
            logger.info("Successfully dropped index '{}' from orders collection", INDEX_NAME);

        } catch (Exception e) {
            logger.warn("Failed to drop index '{}': {}", INDEX_NAME, e.getMessage());
            // Don't rethrow - rollback should be resilient
        }
    }
}