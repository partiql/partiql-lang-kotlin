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
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-types"))
    implementation(Deps.csv)
    implementation(Deps.awsSdkBom)
    implementation(Deps.awsSdkDynamodb)
    implementation(Deps.guava)
    implementation(Deps.jansi)
    implementation(Deps.jline)
    implementation(Deps.joda)
    implementation(Deps.picoCli)
    implementation(Deps.kotlinReflect)
    testImplementation(Deps.mockito)
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1") // Update this line
    testImplementation("io.kotest:kotest-runner-junit5:4.6.0") // Update this line
    testImplementation("io.kotest:kotest-assertions-core:4.6.0")
    testImplementation(project(mapOf("path" to ":plugins:partiql-bananna"))) // Update this line
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

// TODO: Once we upgrade kotlin version to 1.6+, we need to change the compile option to -opt-in
// Version 1.7+ removes the requirement for such compiler option.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions
        .freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
