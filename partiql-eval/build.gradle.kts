import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

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
    id(Plugins.kotlinLombok) version Versions.kotlinLombok
}

dependencies {
    api(project(":partiql-ast"))
    api(project(":partiql-plan"))
    api(project(":partiql-planner"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
    // Test
    testImplementation(project(":partiql-parser"))
    testImplementation(testFixtures(project(":partiql-types"))) // TODO: Remove use of StaticType
    testImplementation(testFixtures(project(":partiql-spi")))
    testImplementation(Deps.junit4)
    testImplementation(Deps.junit4Params)
    testImplementation(Deps.junitVintage) // Enables JUnit4
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

// TODO: Figure out why this is needed.
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

publish {
    artifactId = "partiql-eval"
    name = "PartiQL Lang Kotlin Evaluator"
    description = "The PartiQL reference implementation evaluator."
}

tasks.processTestResources {
    dependsOn(":partiql-planner:generateResourcePath")
    from("${project(":partiql-planner").buildDir}/resources/testFixtures")
}
