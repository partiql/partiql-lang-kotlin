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
    "lib:partiql-isl",
    "test:partiql-tests-runner",
    "test:partiql-randomized-tests",
    "version-upgrade:v0.1-to-v0.2-upgrade:examples",
    "version-upgrade:v0.1-to-v0.2-upgrade:upgraded-examples",
    "version-upgrade:v0.2-to-v0.3-upgrade:examples",
    "version-upgrade:v0.2-to-v0.3-upgrade:upgraded-examples",
    "version-upgrade:v0.3-to-v0.4-upgrade:examples",
    "version-upgrade:v0.3-to-v0.4-upgrade:upgraded-examples",
    "version-upgrade:v0.4-to-v0.5-upgrade:examples",
    "version-upgrade:v0.4-to-v0.5-upgrade:upgraded-examples",
    "version-upgrade:v0.5-to-v0.6-upgrade:examples",
    "version-upgrade:v0.5-to-v0.6-upgrade:upgraded-examples",
    "version-upgrade:v0.6-to-v0.7-upgrade:examples",
    "version-upgrade:v0.6-to-v0.7-upgrade:upgraded-examples",
    "version-upgrade:v0.7-to-v0.8-upgrade:examples",
    "version-upgrade:v0.7-to-v0.8-upgrade:upgraded-examples"
)
