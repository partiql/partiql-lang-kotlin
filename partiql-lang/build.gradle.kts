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
    id(Plugins.antlr)
    id(Plugins.conventions)
    id(Plugins.jmh) version Versions.jmh
    id(Plugins.library)
    id(Plugins.pig)
    id(Plugins.publish)
}

// Disabled for partiql-lang project.
kotlin {
    explicitApi = null
}

dependencies {
    antlr(Deps.antlr)
    api(project(":lib:isl"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    api(project(":partiql-plan"))
    api(Deps.ionElement)
    api(Deps.ionJava)
    api(Deps.pigRuntime)
    implementation(Deps.antlrRuntime)
    implementation(Deps.csv)
    implementation(Deps.kotlinReflect)

    testImplementation(project(":plugins:partiql-mockdb"))
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

pig {
    namespace = "org.partiql.lang.domains"
}

tasks.generateGrammarSource {
    val antlrPackage = "org.partiql.lang.syntax.antlr"
    val antlrSources = "$buildDir/generated-src/${antlrPackage.replace('.', '/')}"
    maxHeapSize = "64m"
    arguments = listOf("-visitor", "-long-messages", "-package", antlrPackage)
    outputDirectory = File(antlrSources)
}

jmh {
    resultFormat = properties["resultFormat"] as String? ?: "json"
    resultsFile = project.file(properties["resultsFile"] as String? ?: "$buildDir/reports/jmh/results.json")
    include = listOfNotNull(properties["include"] as String?)
    properties["warmupIterations"]?.let { it -> warmupIterations = Integer.parseInt(it as String) }
    properties["iterations"]?.let { it -> iterations = Integer.parseInt(it as String) }
    properties["fork"]?.let { it -> fork = Integer.parseInt(it as String) }
}

tasks.javadoc {
    exclude("**/antlr/**")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.findByName("sourcesJar")?.apply {
    dependsOn(tasks.generateGrammarSource)
}

tasks.dokkaHtml.configure {
    dependsOn(tasks.withType(org.partiql.pig.gradle.PigTask::class))
}

tasks.processResources {
    from("src/main/antlr") {
        include("**/*.g4")
    }
    from("src/main/pig") {
        include("partiql.ion")
        into("org/partiql/type-domains/")
    }
}
