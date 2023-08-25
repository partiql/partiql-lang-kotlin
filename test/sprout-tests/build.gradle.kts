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
}

dependencies {
    api(Deps.ionElement)
    api(project(":partiql-types"))
}

val generate = tasks.register<Exec>("generate") {
    dependsOn(":lib:sprout:install")
    workingDir(projectDir)
    commandLine(
        "../../lib/sprout/build/install/sprout/bin/sprout", "generate", "kotlin",
        "-o", "$buildDir/generated-src",
        "-p", "org.partiql.sprout.tests.example",
        "-u", "Example",
        "--poems", "visitor",
        "--poems", "builder",
        "--poems", "util",
        "./src/main/resources/example.ion"
    )
}

tasks.compileKotlin {
    dependsOn(generate)
}
