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
