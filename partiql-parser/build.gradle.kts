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
    id(Plugins.shadowPlugin)
    id(Plugins.kotlinLombok) version Versions.kotlinLombok
}

dependencies {
    antlr(Deps.antlr)
    api(project(":partiql-ast"))
    api(project(":partiql-spi"))
    implementation(Deps.ionElement)
    shadow(Deps.antlrRuntime)
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
}

val relocations = mapOf(
    "org.antlr" to "org.partiql.parser.thirdparty.antlr"
)

tasks.shadowJar {
    dependsOn(tasks.named("generateGrammarSource"))
    configurations = listOf(project.configurations.shadow.get())

    // Set classifier to distinguish shadowed artifacts
    archiveClassifier.set("shadow")

    // Relocate org.partiql packages to shadow.org.partiql
    relocate(Namespace.orgPartiql, Namespace.shadowOrgPartiql)

    // Apply existing relocations for third-party dependencies
    for ((from, to) in relocations) {
        relocate(from, to)
    }

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

tasks.generateGrammarSource {
    val antlrPackage = "org.partiql.parser.internal.antlr"
    val antlrSources = "${layout.buildDirectory.get()}/generated-src/${antlrPackage.replace('.', '/')}"
    maxHeapSize = "64m"
    arguments = listOf("-visitor", "-long-messages", "-package", antlrPackage)
    outputDirectory = File(antlrSources)
}

apiValidation {
    ignoredPackages.addAll(
        listOf(
            "org.partiql.parser.internal"
        )
    )
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
    // TODO remove in next major version release.
    rename("PartiQLParser.g4", "PartiQL.g4")
}

publish {
    artifactId = "partiql-parser-shadow"
    name = "PartiQL Parser"
    description = "PartiQL's Parser"
    // `antlr` dependency configuration adds the ANTLR API configuration (and Maven `compile` dependency scope on
    // publish). It's a known issue w/ the ANTLR gradle plugin. Follow https://github.com/gradle/gradle/issues/820
    // for context. In the maven publishing step, any API or IMPLEMENTATION dependencies w/ "antlr4" non-runtime
    // dependency will be omitted from the created Maven POM.
    excludedDependencies = setOf("antlr4")
}
