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
    id(Plugins.publish)
    id(Plugins.library)
}

dependencies {
    api(project(":partiql-types"))
    implementation(Deps.ionElement)
    implementation(Deps.kotlinReflect)
}

// Disabled for partiql-plan project.
kotlin {
    explicitApi = null
}

publish {
    artifactId = "partiql-plan"
    name = "PartiQL Plan"
    description = "PartiQL Plan experimental data structures"
}

val generate = tasks.register<Exec>("generate") {
    dependsOn(":lib:sprout:install")
    workingDir(projectDir)
    commandLine(
        "../lib/sprout/build/install/sprout/bin/sprout",
        "generate",
        "kotlin",
        "-o", "$buildDir/generated-src",
        "-p", "org.partiql.plan",
        "-u", "Plan",
        "--poems", "factory",
        "--poems", "visitor",
        "--poems", "builder",
        "--poems", "util",
        "--opt-in", "org.partiql.value.PartiQLValueExperimental",
        "./src/main/resources/partiql_plan_0_1.ion"
    )
}

tasks.compileKotlin {
    dependsOn(generate)
}
