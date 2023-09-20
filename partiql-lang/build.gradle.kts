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
    id(Plugins.jmh) version Versions.jmh
    id(Plugins.library)
    id(Plugins.publish)
}

// Disabled for partiql-lang project.
kotlin {
    explicitApi = null
}

dependencies {
    api(project(":partiql-ast"))
    api(project(":partiql-parser"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    api(project(":partiql-plan"))
    api(Deps.ionElement)
    api(Deps.ionJava)
    api(Deps.ionSchema)
    implementation(Deps.antlrRuntime)
    implementation(Deps.csv)
    implementation(Deps.kotlinReflect)

    testImplementation(project(":plugins:partiql-mockdb"))
    testImplementation(project(":lib:isl"))
    testImplementation(Deps.assertj)
    testImplementation(Deps.junit4)
    testImplementation(Deps.junit4Params)
    testImplementation(Deps.junitVintage) // Enables JUnit4
    testImplementation(Deps.mockk)
}

publish {
    artifactId = "partiql-lang-kotlin"
    name = "PartiQL Lang Kotlin"
    description = "An implementation of PartiQL for the JVM written in Kotlin."
}

jmh {
    resultFormat = properties["resultFormat"] as String? ?: "json"
    resultsFile = project.file(properties["resultsFile"] as String? ?: "$buildDir/reports/jmh/results.json")
    include = listOfNotNull(properties["include"] as String?)
    properties["warmupIterations"]?.let { it -> warmupIterations = Integer.parseInt(it as String) }
    properties["iterations"]?.let { it -> iterations = Integer.parseInt(it as String) }
    properties["fork"]?.let { it -> fork = Integer.parseInt(it as String) }
}

tasks.processResources {
    // include .g4 in partiql-lang-kotlin JAR for backwards compatibility
    from("$rootDir/partiql-parser/src/main/antlr") {
        include("**/*.g4")
    }
    // include partiql.ion in partiql-lang-kotlin JAR for backwards compatibility
    from("$rootDir/partiql-ast/src/main/pig") {
        include("partiql.ion")
    }
}
