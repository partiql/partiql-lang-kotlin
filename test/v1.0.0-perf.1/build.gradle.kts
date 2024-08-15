/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

plugins {
    id(Plugins.conventions)
    id(Plugins.jmh) version Versions.jmhGradlePlugin
    id(Plugins.library)
    id(Plugins.publish)
}

dependencies {
    implementation("org.partiql:partiql-eval:1.0.0-perf.1")
    implementation("org.partiql:partiql-planner:1.0.0-perf.1")
    implementation("org.partiql:partiql-parser:1.0.0-perf.1")
    implementation("org.partiql:plugin-memory:1.0.0-perf.1")
}

tasks.shadowJar {
    relocate("org.partiql", "org.partiql_v1_0_0_perf_1")
}
