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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.gradle.jacoco")
    id("org.jlleitschuh.gradle.ktlint")
}

val generatedSrc = "$buildDir/generated-src"
val generatedVersion = "$buildDir/generated-version"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(Deps.kotlin)
    testImplementation(Deps.kotlinTest)
    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.junitParams)
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.jvmTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.jvmTarget)
}

tasks.test {
    useJUnitPlatform() // Enable JUnit5
    jvmArgs.addAll(listOf("-Duser.language=en", "-Duser.country=US"))
    maxHeapSize = "4g"
    testLogging {
        events.add(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
    dependsOn(tasks.ktlintCheck) // check style before unit tests
    finalizedBy(tasks.jacocoTestReport)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
}

configure<KtlintExtension> {
    filter {
        exclude { it.file.path.contains(generatedSrc) }
    }
}

sourceSets {
    main {
        java.srcDir(generatedSrc)
        output.dir(generatedVersion)
    }
}

kotlin.sourceSets {
    all {
        languageSettings.optIn("kotlin.RequiresOptIn")
    }
    main {
        kotlin.srcDir(generatedSrc)
    }
}

detekt {
    parallel = true
    debug = true
    ignoreFailures = false
    config = files(rootProject.projectDir.resolve("detekt-config.yml"))
}

tasks.detekt {
    reports {
        html.required.set(true)
        txt.required.set(true)
        xml.required.set(true)
    }
}

jacoco {
    toolVersion = Versions.jacoco
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JacocoReport> {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it).apply {
                // Improve this — generated source exclusions should be automatic
                exclude(
                    "org/partiql/lang/domains/*.class",
                    "org/partiql/ionschema/model/*.class",
                )
            }
        }))
        // TODO: Figure this out.
//        val files = classDirectories.files.map { fileTree(it) }
//        val partiqlDir = buildDir.resolve("partiql")
//        println("PartiQL Directory: $partiqlDir")
//        val queryFiles = fileTree(partiqlDir)
//        println("PartiQL Tree: $queryFiles")
//        println("PartiQL Tree Elements: ${queryFiles.elements}")
//        val allFiles = files + listOf(queryFiles)
//        println("All Files: $allFiles")
//        sourceDirectories.setFrom(
//            files(allFiles)
//        )
//        sourceSets {
//            main {
//                files(partiqlDir)
//            }
//        }
//        sourceSets(sourceSets.main.get())
//        classDirectories.setFrom(
//            files(allFiles)
//        )
    }
}

// TODO: How to create custom source set
//tasks.create<Test>("integrationTest") {
//    description = "Runs Integration Tests"
//    testClassesDirs = sourceSets.integrationTest.output.classesDir
//    classpath += sourceSets.integrationTest.runtimeClasspath
//}

tasks.processResources {
    dependsOn(tasks.findByName("generateVersionAndHash"))
}

tasks.create("generateVersionAndHash") {
    val propertiesFile = file("$generatedVersion/partiql.properties")
    propertiesFile.parentFile.mkdirs()
    val properties = Properties()
    // Version
    val version = version.toString()
    properties.setProperty("version", version)
    // Commit Hash
    val commit = ByteArrayOutputStream().apply {
        exec {
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = this@apply
        }
    }.toString().trim()
    properties.setProperty("commit", commit)
    // Write file
    val out = FileOutputStream(propertiesFile)
    properties.store(out, null)
}
