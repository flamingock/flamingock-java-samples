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

package io.flamingock.examples.inventory;

import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import io.flamingock.community.Flamingock;
import io.flamingock.community.mongodb.sync.driver.MongoDBSyncAuditStore;
import io.flamingock.examples.inventory.util.TargetSystems;
import io.flamingock.internal.core.store.CommunityAuditStore;
import io.flamingock.targetystem.mongodb.sync.MongoDBSyncTargetSystem;

import static io.flamingock.examples.inventory.util.TargetSystems.DATABASE_NAME;

@EnableFlamingock(
        stages = {
                @Stage(name = "inventory", location = "io.flamingock.examples.inventory.changes")
        }
)
public class InventoryOrdersApp {


    public static void main(String[] args) throws Exception {
        Flamingock.builder()
                .setAuditStore(auditStore())
                .addTargetSystems(
                        TargetSystems.mongoDBSyncTargetSystem(),
                        TargetSystems.kafkaTargetSystem(),
                        TargetSystems.toggleTargetSystem())
                .build()
                .run();

    }

    //This could return any of the available community audit stores
    private static CommunityAuditStore auditStore() {
        MongoDBSyncTargetSystem targetSystem = TargetSystems.mongoDBSyncTargetSystem();
        return MongoDBSyncAuditStore.from(targetSystem);
    }


}
