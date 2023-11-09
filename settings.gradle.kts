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
    "partiql-ast",
    "partiql-cli",
    "partiql-coverage",
    "partiql-lang",
    "partiql-parser",
    "partiql-plan",
    "partiql-planner",
    "partiql-spi",
    "partiql-types",
    "plugins:partiql-local",
    "plugins:partiql-memory",
    "lib:isl",
    "lib:sprout",
    "test:coverage-tests",
    "test:partiql-tests-runner",
    "test:partiql-randomized-tests",
    "test:sprout-tests",
    "examples",
)
