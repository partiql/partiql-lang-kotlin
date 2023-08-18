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
}

dependencies {
    api(project(":partiql-ast"))
    api(project(":partiql-plan"))
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
}

addTestArtifactsConfiguration()

publish {
    artifactId = "partiql-planner"
    name = "PartiQL Planner"
    description = "PartiQL's Logical Planner"
}

/**
 * Adds a "testArtifacts" configuration to expose the test source to other libraries (for testing purposes)
 */
fun addTestArtifactsConfiguration() {
    configurations.create("testArtifacts").also { it.extendsFrom(configurations.testImplementation.get()) }
    val testJar = tasks.create<Jar>("testJar") {
        archiveClassifier.set("test")
        from(sourceSets.test.get().output)
    }
    artifacts {
        add("testArtifacts", testJar)
    }
}
