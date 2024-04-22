/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

plugins {
    id(Plugins.antlr)
    id(Plugins.conventions)
    id(Plugins.publish)
}

dependencies {
    antlr(Deps.antlr)
    api(project(":partiql-ast"))
    api(project(":partiql-types"))
    implementation(Deps.ionElement)
    implementation(Deps.antlrRuntime)
}

val relocations = mapOf(
    "org.antlr" to "org.partiql.thirdparty.antlr"
)

tasks.shadowJar {
    dependsOn(tasks.named("generateGrammarSource"))
    archiveClassifier.set("shaded")
    for ((from, to) in relocations) {
        relocate(from, to)
    }
}

tasks.generateGrammarSource {
    val antlrPackage = "org.partiql.parser.antlr"
    val antlrSources = "$buildDir/generated-src/${antlrPackage.replace('.', '/')}"
    maxHeapSize = "64m"
    arguments = listOf("-visitor", "-long-messages", "-package", antlrPackage)
    outputDirectory = File(antlrSources)
}

tasks.javadoc {
    exclude("**/antlr/**")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<Jar>().configureEach {
    // ensure "generateGrammarSource" is called before "sourcesJar".
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.runKtlintCheckOverTestSourceSet {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.processResources {
    from("src/main/antlr") {
        include("**/*.g4")
    }
}

publish {
    artifactId = "partiql-parser"
    name = "PartiQL Parser"
    description = "PartiQL's experimental Parser"
}
