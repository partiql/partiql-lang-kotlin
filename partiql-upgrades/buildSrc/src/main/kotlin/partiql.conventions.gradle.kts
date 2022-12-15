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
    kotlin("jvm")
}

object Versions {
    const val kotlin = "1.5.31"
    const val kotlinTarget = "1.4"
    const val javaTarget = "1.8"
}

object Deps {
    val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
}

object Plugins {
    val conventions = "partiql.conventions"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(Deps.kotlin)
    testImplementation(Deps.kotlinTest)
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.javaTarget)
    targetCompatibility = JavaVersion.toVersion(Versions.javaTarget)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.javaTarget
    kotlinOptions.apiVersion = Versions.kotlinTarget
    kotlinOptions.languageVersion = Versions.kotlinTarget
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = Versions.javaTarget
    kotlinOptions.apiVersion = Versions.kotlinTarget
    kotlinOptions.languageVersion = Versions.kotlinTarget
}
