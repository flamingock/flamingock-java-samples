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
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static io.flamingock.examples.inventory.TargetSystems.MONGODB_TARGET_SYSTEM;

@TargetSystem(id = MONGODB_TARGET_SYSTEM)
@Change(id = "add-discount-code-field-to-orders", author = "flamingock-team")
public class _0001__mongodb_addDiscountCodeFieldToOrders {

    private static final String ORDERS_COLLECTION_NAME = "orders";

    @Apply
    public void apply(MongoTemplate mongoTemplate) {
        Document order1 = buildOrder1();
        Document order2 = buildOrder2();
        mongoTemplate
                .getCollection(ORDERS_COLLECTION_NAME)
                .insertMany(Arrays.asList(order1, order2));

    }

    @Rollback
    public void rollback(MongoTemplate mongoTemplate) {
        if(mongoTemplate.collectionExists(ORDERS_COLLECTION_NAME)) {
            mongoTemplate.dropCollection(ORDERS_COLLECTION_NAME);
        }
    }


    private static Document buildOrder2() {
        return new Document()
            .append("orderId", "ORD-002")
            .append("customerId", "CUST-102")
            .append("items", Collections.singletonList(
                    new Document("productId", "PROD-C").append("quantity", 3).append("price", 15.99)
            ))
            .append("total", 47.97)
            .append("status", "COMPLETED")
            .append("createdAt", LocalDateTime.now().toString());
    }

    private static Document buildOrder1() {
        return new Document()
            .append("orderId", "ORD-001")
            .append("customerId", "CUST-101")
            .append("items", Arrays.asList(
                new Document("productId", "PROD-A").append("quantity", 2).append("price", 29.99),
                new Document("productId", "PROD-B").append("quantity", 1).append("price", 49.99)
            ))
            .append("total", 109.97)
            .append("status", "PENDING")
            .append("createdAt", LocalDateTime.now().toString());
    }

}