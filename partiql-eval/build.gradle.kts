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
}

// Disabled for partiql-eval project at initialization.
kotlin {
    explicitApi = null
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

// Need to add this as we have both Java and Kotlin sources. Dokka already handles multi-language projects. If
// Javadoc is enabled, we end up overwriting index.html (causing compilation errors).
tasks.withType<Javadoc>() {
    enabled = false
}
tasks.withType<Jar>() {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
