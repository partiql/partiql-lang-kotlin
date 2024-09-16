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
    id(Plugins.testFixtures)
}

// Disabled for partiql-lang project.
kotlin {
    explicitApi = null
}

dependencies {
    api(project(":partiql-ast"))
    api(project(":partiql-parser", configuration = "shadow"))
    api(project(":partiql-plan"))
    api(project(":partiql-planner"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    //
    api(Deps.ionElement)
    api(Deps.ionJava)
    api(Deps.ionSchema)
    implementation(Deps.csv)
    implementation(Deps.kotlinReflect)
    implementation(Deps.kotlinxCoroutines)

    testImplementation(testFixtures(project(":partiql-planner")))
    testImplementation(project(":plugins:partiql-memory"))
    testImplementation(Deps.assertj)
    testImplementation(Deps.junit4)
    testImplementation(Deps.junit4Params)
    testImplementation(Deps.junitVintage) // Enables JUnit4
    testImplementation(Deps.mockk)
    testImplementation(Deps.kotlinxCoroutinesTest)

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

tasks.compileTestFixturesKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
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

apiValidation {
    /**
     * Classes (fully qualified) that are excluded from public API dumps even if they
     * contain public API.
     */
    ignoredClasses.addAll(
        listOf(
            "org.partiql.lang.compiler.PartiQLCompilerPipeline" // deprecated
        )
    )
}

publish {
    artifactId = "partiql-lang-kotlin"
    name = "PartiQL Lang Kotlin"
    description = "An implementation of PartiQL for the JVM written in Kotlin."
}

jmh {
    resultFormat = properties["resultFormat"] as String? ?: "json"
    resultsFile = project.file(properties["resultsFile"] as String? ?: "$buildDir/reports/jmh/results.json")
    includes = listOfNotNull(properties["include"] as String?)
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

tasks.processTestResources {
    dependsOn(":partiql-planner:generateResourcePath")
    from("${project(":partiql-planner").buildDir}/resources/testFixtures")
}
