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
import io.flamingock.examples.inventory.util.LaunchDarklyClient;

import static io.flamingock.examples.inventory.TargetSystems.FEATURE_FLAG_TARGET_SYSTEM;

@TargetSystem(id = FEATURE_FLAG_TARGET_SYSTEM)
@Change(id = "add-feature-flag-discounts", author = "flamingock-team", transactional = false)
public class _0003__toggle_addFeatureFlagDiscounts {

    @Apply
    public void apply(LaunchDarklyClient launchDarkly) throws Exception {

        // Create main enable/disable flag for the discount feature
        launchDarkly.createBooleanFlag(
            "enable-discounts",
            "Enable Discount System",
            "Controls whether discount codes are enabled for orders"
        );

        // Create string flag for discount code configuration
        launchDarkly.createStringFlag(
            "discount-codes",
            "Available Discount Codes",
            "Available discount codes for the system",
            new String[]{"NONE", "SUMMER10", "WELCOME15", "LOYAL20"}
        );

        // Create string flag for max discount percentage (as string to allow easy updates)
        launchDarkly.createStringFlag(
            "max-discount-percent",
            "Maximum Discount Percentage",
            "Maximum allowed discount percentage",
            new String[]{"10", "15", "20", "25"}
        );

    }

    @Rollback
    public void rollback(LaunchDarklyClient launchDarkly) throws Exception {
        // Delete all the flags that were created in the apply method
        launchDarkly.deleteFlag("enable-discounts");
        launchDarkly.deleteFlag("discount-codes");
        launchDarkly.deleteFlag("max-discount-percent");

    }
}