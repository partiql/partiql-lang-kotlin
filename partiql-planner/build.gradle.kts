import org.jetbrains.dokka.utilities.relativeTo

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
}

dependencies {
    api(project(":partiql-plan"))
    api(project(":partiql-types"))
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-spi"))
    implementation(Deps.dotlin)
    implementation(Deps.ionElement)
    // Test
    testImplementation(project(":partiql-parser"))
    testImplementation(project(":plugins:partiql-local"))
    testImplementation(project(":plugins:partiql-memory"))
    testImplementation(project(":plugins:partiql-base-jdbc"))
    testImplementation("org.postgresql:postgresql:42.7.3")
    // Test Fixtures
    testFixturesImplementation(project(":partiql-spi"))
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
                    val toAppend = file.toURI().relativeTo(resourceDir.toURI())
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

publish {
    artifactId = "partiql-planner"
    name = "PartiQL Planner"
    description = "PartiQL's Experimental Planner."
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
        "--opt-in", "org.partiql.value.PartiQLValueExperimental",
        "--opt-in", "org.partiql.spi.fn.FnExperimental",
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
tasks.register<Copy>("copyNodes") {
    includeEmptyDirs = false
    dependsOn("codegen")
    filter { it.replace(Regex("public (?!(override|(fun visit)))"), "internal ") }
    from("$buildDir/tmp")
    include("**/Nodes.kt")
    into("src/main/kotlin")
}

tasks.register("generate") {
    dependsOn("codegen", "copyUtils")
}

tasks.compileKotlin {
    dependsOn("generate")
}
