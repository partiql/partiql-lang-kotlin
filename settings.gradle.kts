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
    "lang",
    "partiql-app:partiql-cli",
    "examples",
    "extensions",
    "partiql-lib:partiql-isl",
    "partiql-lib:partiql-sprout",
    "test:partiql-tests-runner",
    "test:partiql-randomized-tests",
)
