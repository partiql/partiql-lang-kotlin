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
    api(project(":partiql-ast"))
    api(project(":partiql-plan"))
    api(project(":partiql-planner"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    // Test
    testImplementation(project(":lib:isl"))
    testImplementation(project(":partiql-parser"))
    testImplementation(project(":plugins:partiql-local"))
    testImplementation(project(":plugins:partiql-memory"))
    testImplementation(testFixtures(project(":partiql-planner")))
    testImplementation(testFixtures(project(":partiql-lang")))
    testImplementation(Deps.junit4)
    testImplementation(Deps.junit4Params)
    testImplementation(Deps.junitVintage) // Enables JUnit4
    testImplementation(project(":plugins:partiql-base-jdbc"))
    testImplementation("org.postgresql:postgresql:42.7.3")
}

// Disabled for partiql-eval project at initialization.
kotlin {
    explicitApi = null
}

publish {
    artifactId = "partiql-eval"
    name = "PartiQL Lang Kotlin Evaluator"
    description = "Experimental PartiQL plan-based evaluator"
}

tasks.processTestResources {
    dependsOn(":partiql-planner:generateResourcePath")
    from("${project(":partiql-planner").buildDir}/resources/testFixtures")
}
