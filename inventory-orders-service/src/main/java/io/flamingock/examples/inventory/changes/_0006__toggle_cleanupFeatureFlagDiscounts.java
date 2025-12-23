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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.flamingock.examples.inventory.TargetSystems.FEATURE_FLAG_TARGET_SYSTEM;

@TargetSystem(id = FEATURE_FLAG_TARGET_SYSTEM)
@Change(id = "cleanup-feature-flag-discounts", author = "flamingock-team", transactional = false)
public class _0006__toggle_cleanupFeatureFlagDiscounts {

    private static final Logger logger = LoggerFactory.getLogger(_0006__toggle_cleanupFeatureFlagDiscounts.class);

    @Apply
    public void apply(LaunchDarklyClient launchDarkly) throws Exception {
        logger.info("Cleaning up discount feature flags from LaunchDarkly");

        // Archive flags instead of deleting them (best practice for historical tracking)
        // The enable-discounts flag is no longer needed since the feature is now permanent
        launchDarkly.archiveFlag("enable-discounts");

        // Keep configuration flags but archive temporary rollout flags
        // discount-codes and max-discount-percent would typically remain for ongoing config
        // but for this demo we'll show cleanup of temporary flags
        launchDarkly.archiveFlag("discount-codes");
        launchDarkly.archiveFlag("max-discount-percent");

        logger.info("Discount feature is now permanent - temporary flags have been archived");
        logger.info("Permanent discount configuration would remain in application settings");
    }

    @Rollback
    public void rollback(LaunchDarklyClient launchDarkly) throws Exception {
        logger.info("Rolling back: Recreating discount feature flags");

        // Recreate the flags that were archived in the apply method
        launchDarkly.createBooleanFlag(
            "enable-discounts",
            "Enable Discount System",
            "Controls whether discount codes are enabled for orders"
        );

        launchDarkly.createStringFlag(
            "discount-codes",
            "Available Discount Codes",
            "Available discount codes for the system",
            new String[]{"NONE", "SUMMER10", "WELCOME15", "LOYAL20"}
        );

        launchDarkly.createStringFlag(
            "max-discount-percent",
            "Maximum Discount Percentage",
            "Maximum allowed discount percentage",
            new String[]{"10", "15", "20", "25"}
        );

        logger.info("Discount feature flags restored for continued rollout management");
    }
}