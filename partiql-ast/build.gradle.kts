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
    id(Plugins.pig)
}

dependencies {
    api(Deps.pigRuntime)
    api(Deps.ionElement)
    implementation(project(":partiql-types"))
}

publish {
    artifactId = "partiql-ast"
    name = "PartiQL AST"
    description = "PartiQL's Abstract Syntax Tree"
}

pig {
    namespace = "org.partiql.lang.domains"
}

tasks.dokkaHtml.configure {
    dependsOn(tasks.withType(org.partiql.pig.gradle.PigTask::class))
}

tasks.processResources {
    from("src/main/pig") {
        include("partiql.ion")
        into("org/partiql/type-domains/")
    }
}

kotlin {
    // TODO: Once PIG is either removed or adds explicit visibility modifiers, we can remove this.
    //  See https://github.com/partiql/partiql-ir-generator/issues/108.
    explicitApi = null
}

val generate = tasks.register<Exec>("generate") {
    dependsOn(":lib:sprout:install")
    workingDir(projectDir)
    commandLine(
        "../lib/sprout/build/install/sprout/bin/sprout", "generate", "kotlin",
        "-o", "$buildDir/generated-src",
        "-p", "org.partiql.ast",
        "-u", "Ast",
        "--poems", "visitor",
        "--poems", "builder",
        "--poems", "util",
        "--opt-in", "org.partiql.value.PartiQLValueExperimental",
        "./src/main/resources/partiql_ast.ion"
    )
}

tasks.compileKotlin {
    dependsOn(generate)
}
