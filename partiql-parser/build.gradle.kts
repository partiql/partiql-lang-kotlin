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
    id(Plugins.publish)
}

dependencies {
    antlr(Deps.antlr)
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-types"))
    implementation(Deps.ionElement)
    implementation(Deps.antlrRuntime)
}

publish {
    artifactId = "partiql-parser"
    name = "PartiQL Parser"
    description = "The PartiQL Parser"
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

tasks.findByName("sourcesJar")?.apply {
    dependsOn(tasks.generateGrammarSource)
}

tasks.processResources {
    from("src/main/antlr") {
        include("**/*.g4")
    }
}
