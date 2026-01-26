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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
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
@Change(id = "backfill-discounts-for-existing-orders", author = "flamingock-team", transactional = true)
public class _0004__mongodb_backfillDiscountsForExistingOrders {

    private static final Logger logger = LoggerFactory.getLogger(_0004__mongodb_backfillDiscountsForExistingOrders.class);

    @Apply
    public void apply(MongoTemplate mongoTemplate) {
        logger.info("Backfilling discountCode field for existing orders");

        MongoCollection<Document> orders = mongoTemplate.getCollection("orders");

        // Update all orders that don't have a discountCode field
        var filter = Filters.exists("discountCode", false);
        var update = Updates.set("discountCode", "NONE");

        var result = orders.updateMany(filter, update);

        logger.info("Backfilled {} orders with default discountCode='NONE'", result.getModifiedCount());

        // Also add discountApplied field to track if discount was actually applied
        orders.updateMany(
            Filters.exists("discountApplied", false),
            Updates.set("discountApplied", false)
        );

        logger.info("Added discountApplied field to track discount application status");
    }

    @Rollback
    public void rollback(MongoTemplate mongoTemplate) {
        logger.info("Rolling back: Removing discountCode and discountApplied fields");

        MongoCollection<Document> orders = mongoTemplate.getCollection("orders");

        // Remove the discountCode field from all documents
        orders.updateMany(
            Filters.exists("discountCode"),
            Updates.unset("discountCode")
        );

        // Remove the discountApplied field from all documents
        orders.updateMany(
            Filters.exists("discountApplied"),
            Updates.unset("discountApplied")
        );

        logger.info("Removed discount-related fields from all orders");
    }
}