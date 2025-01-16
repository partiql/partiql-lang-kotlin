import org.jetbrains.dokka.gradle.DokkaTask
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath

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
    id(Plugins.testFixtures)
    id(Plugins.publish)
    id(Plugins.kotlinLombok) version Versions.kotlinLombok
}

dependencies {
    api(project(":partiql-plan"))
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-spi"))
    implementation(Deps.dotlin)
    implementation(Deps.ionElement)
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
    // Test
    testImplementation(project(":partiql-parser"))
    testImplementation(Deps.kotlinReflect)
    // Test Fixtures
    testImplementation(testFixtures(project(":partiql-spi")))
    testFixturesImplementation(project(":partiql-spi"))
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

tasks.register("generateResourcePath") {
    dependsOn("processTestFixturesResources")
    doLast {
        val resourceDir = file("src/testFixtures/resources")
        val outDir = File("$buildDir/resources/testFixtures")
        val fileName = "resource_path.txt"
        val pathFile = File(outDir, fileName)
        if (pathFile.exists()) {
            pathFile.writeText("") // clean up existing text
        }
        resourceDir.walk().forEach { file ->
            if (!file.isDirectory) {
                if (file.extension == "ion" || file.extension == "sql") {
                    val toAppend = file.toURI().toPath().relativeTo(resourceDir.toURI().toPath())
                    pathFile.appendText("$toAppend\n")
                }
            }
        }

        sourceSets {
            testFixtures {
                resources {
                    this.srcDirs += pathFile
                }
            }
        }
    }
}

tasks.processTestResources {
    dependsOn("generateResourcePath")
    from("src/testFixtures/resources")
}

tasks.compileTestFixturesKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
}

publish {
    artifactId = "partiql-planner"
    name = "PartiQL Planner"
    description = "PartiQL's Planner."
}

// Generate internal IR
tasks.register<Exec>("codegen") {
    dependsOn(":lib:sprout:install")
    workingDir(projectDir)
    commandLine(
        "../lib/sprout/build/install/sprout/bin/sprout",
        "generate",
        "kotlin",
        "-o", "$buildDir/tmp",
        "-p", "org.partiql.planner.internal.ir",
        "-u", "Plan",
        "--poems", "factory",
        "--poems", "visitor",
        "--poems", "builder",
        "--poems", "util",
        "./src/main/resources/partiql_plan_internal.ion"
    )
}

// Copy generated utilities to generated-src
tasks.register<Copy>("copyUtils") {
    includeEmptyDirs = false
    dependsOn("codegen")
    filter { it.replace(Regex("public (?!(override|(fun visit)))"), "internal ") }
    from("$buildDir/tmp")
    exclude("**/Nodes.kt")
    into("$buildDir/generated-src")
}

// Copy generated Nodes.kt to src
//
// !! IMPORTANT !! — only run manually, as this will overwrite the existing ir/Nodes.kt.
//
// tasks.register<Copy>("copyNodes") {
//     includeEmptyDirs = false
//     dependsOn("codegen")
//     filter { it.replace(Regex("public (?!(override|(fun visit)))"), "internal ") }
//     from("$buildDir/tmp")
//     include("**/Nodes.kt")
//     into("src/main/kotlin")
// }

tasks.register("generate") {
    dependsOn("codegen", "copyUtils")
}

tasks.compileKotlin {
    dependsOn("generate")
    dependsOn(tasks.withType<Copy>())
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask>().configureEach {
    dependsOn(tasks.withType<Copy>())
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask>().configureEach {
    dependsOn(tasks.withType<Copy>())
}

tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<Copy>())
}

tasks.detekt {
    dependsOn(tasks.withType<Copy>())
}

tasks.withType<DokkaTask>().configureEach {
    dependsOn(tasks.withType<Copy>())
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("Nodes.kt") }
    }
}
