/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License').
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

rootProject.name = "partiql"

include(
    ":custom-ktlint-rules",
    "partiql-ast",
    "partiql-cli",
    "partiql-coverage",
    "partiql-eval",
    "partiql-lang",
    "partiql-parser",
    "partiql-plan",
    "partiql-planner",
    "partiql-spi",
    "lib:sprout",
    "test:coverage-tests",
    "test:partiql-tests-runner",
    "test:partiql-randomized-tests",
    "test:sprout-tests",
    "examples",
    // migration guides
    "docs:upgrades:v0.1-to-v0.2-upgrade:examples",
    "docs:upgrades:v0.1-to-v0.2-upgrade:upgraded-examples",
    "docs:upgrades:v0.2-to-v0.3-upgrade:examples",
    "docs:upgrades:v0.2-to-v0.3-upgrade:upgraded-examples",
    "docs:upgrades:v0.3-to-v0.4-upgrade:examples",
    "docs:upgrades:v0.3-to-v0.4-upgrade:upgraded-examples",
    "docs:upgrades:v0.4-to-v0.5-upgrade:examples",
    "docs:upgrades:v0.4-to-v0.5-upgrade:upgraded-examples",
    "docs:upgrades:v0.5-to-v0.6-upgrade:examples",
    "docs:upgrades:v0.5-to-v0.6-upgrade:upgraded-examples",
    "docs:upgrades:v0.6-to-v0.7-upgrade:examples",
    "docs:upgrades:v0.6-to-v0.7-upgrade:upgraded-examples",
    "docs:upgrades:v0.7-to-v0.8-upgrade:examples",
    "docs:upgrades:v0.7-to-v0.8-upgrade:upgraded-examples",
    "docs:upgrades:v0.14-to-v1-upgrade:examples",
    "docs:upgrades:v0.14-to-v1-upgrade:upgraded-examples"
)
