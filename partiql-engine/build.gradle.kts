

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
 *
 */

plugins {
    id(Plugins.conventions)
    id(Plugins.library)
    id(Plugins.publish)
}

dependencies {
    api(project(":partiql-plan"))
    api(project(":partiql-types"))
    api(project(":partiql-spi"))
    implementation(Deps.ionElement)
    implementation(Deps.pigRuntime)
    // Test
    testImplementation(project(":partiql-parser"))
    testImplementation(project(":partiql-planner"))
    testImplementation(project(":plugins:partiql-local"))
    testImplementation(project(":plugins:partiql-memory"))
}

publish {
    artifactId = "partiql-engine"
    name = "PartiQL Engine"
    description = "PartiQL's Experimental Execution Engine."
}
