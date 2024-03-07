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
    id(Plugins.application)
    id(Plugins.conventions)
}

dependencies {
    implementation(project(":partiql-lang"))
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-parser"))
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-planner"))
    implementation(project(":partiql-types"))
    implementation(project(":plugins:partiql-local"))
    implementation(project(":partiql-spi"))
    implementation(Deps.csv)
    implementation(Deps.awsSdkBom)
    implementation(Deps.awsSdkDynamodb)
    implementation(Deps.guava)
    implementation(Deps.jansi)
    implementation(Deps.jline)
    implementation(Deps.joda)
    implementation(Deps.picoCli)
    implementation(Deps.kotlinReflect)
    implementation(Deps.kotlinxCoroutines)
    testImplementation(Deps.mockito)
}

application {
    applicationName = "partiql"
    mainClass.set("org.partiql.cli.Main")
}

distributions {
    main {
        distributionBaseName.set("partiql-cli")
        contents {
            from("archive") // include contents of `archive/` in the distribution archive
        }
    }
}

tasks.named<Tar>("distTar") {
    compression = Compression.GZIP
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}

val testingPluginDirectory = "$buildDir/tmp/plugins"
val mockDBPluginDirectory = "$testingPluginDirectory/local"

tasks.register<Copy>("generateLocalPluginJar") {
    dependsOn(":plugins:partiql-local:assemble")
    from("${rootProject.projectDir}/plugins/partiql-local/build/libs")
    into(mockDBPluginDirectory)
    include("partiql-local-*.jar")
}

tasks.test.configure {
    dependsOn(tasks.findByName("generateLocalPluginJar"))
}

tasks.withType<Test>().configureEach {
    systemProperty("testingPluginDirectory", testingPluginDirectory)
}

// Version 1.7+ removes the requirement for such compiler option.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions
        .freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
