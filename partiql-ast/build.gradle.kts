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
    id(Plugins.publish)
    id(Plugins.shadowPlugin)
    // Need the Kotlin lombok plugin to allow for Kotlin code in partiql-ast to understand Java Lombok annotations.
    // https://kotlinlang.org/docs/lombok.html
    id(Plugins.kotlinLombok) version Versions.kotlinLombok
}

dependencies {
    api(Deps.ionElement)
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
}

// TODO: Figure out why this is needed.
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    // Set classifier to distinguish shadowed artifacts
    archiveClassifier.set("shadow")

    // Relocate all org.partiql packages to shadow.org.partiql
    relocate(Namespace.orgPartiql, Namespace.shadowOrgPartiql)

    // Merge service files to avoid conflicts
    mergeServiceFiles()
}

// Ensure shadow JAR is built with the main build
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

publish {
    artifactId = "partiql-ast-shadow"
    name = "PartiQL AST"
    description = "PartiQL's Abstract Syntax Tree"
}
