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
    id(Plugins.publish)
    id(Plugins.library)
    id(Plugins.shadowPlugin)
}

dependencies {
    api(project(":partiql-spi"))
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())

    // Set classifier to distinguish shadowed artifacts
    archiveClassifier.set("shadow")

    // Relocate org.partiql packages to shadow.org.partiql
    relocate(Namespace.orgPartiql, Namespace.shadowOrgPartiql)

    // Merge service files to avoid conflicts
    mergeServiceFiles()
}

// Ensure shadow JAR is built with the main build
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.withType<Javadoc> {
    enabled = false
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

publish {
    artifactId = "partiql-plan-shadow"
    name = "PartiQL Plan"
    description = "PartiQL logical plan data structures"
}
