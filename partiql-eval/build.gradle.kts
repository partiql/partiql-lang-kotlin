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
    id(Plugins.testFixtures)
}

dependencies {
    api(project(":partiql-ast"))
    api(project(":partiql-plan"))
    api(project(":partiql-planner"))
    api(project(":partiql-types"))
    implementation(project(":partiql-lang"))
    // Test
    testImplementation(project(":lib:isl"))
    testImplementation(project(":partiql-parser"))
    testImplementation(project(":plugins:partiql-local"))
    testImplementation(project(":plugins:partiql-memory"))
    testImplementation(testFixtures(project(":partiql-planner")))
    testImplementation(Deps.junit4)
    testImplementation(Deps.junit4Params)
    testImplementation(Deps.junitVintage) // Enables JUnit4

    testFixturesImplementation(project(":partiql-lang")) // To be decoupled
    testFixturesImplementation(project(":lib:isl"))
    testFixturesImplementation(Deps.kotlinTest)
    testFixturesImplementation(Deps.kotlinTestJunit)
    testFixturesImplementation(Deps.assertj)
    testFixturesImplementation(Deps.junit4)
    testFixturesImplementation(Deps.junit4Params)
    testFixturesImplementation(Deps.junitApi)
    testFixturesImplementation(Deps.junitParams)
    testFixturesImplementation(Deps.junitVintage) // Enables JUnit4
    testFixturesImplementation(Deps.mockk)
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
