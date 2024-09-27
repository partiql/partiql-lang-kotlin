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
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
}

object Versions {
    const val binaryCompatibilityValidator = "0.14.0"
    const val detekt = "1.20.0-RC2"
    const val dokka = "1.9.20"
    const val kotlin = "1.9.20"
    const val ktlintGradle = "10.2.1"
    const val nexusPublish = "2.0.0"
    const val shadow = "8.1.1"
}

object Plugins {
    const val binaryCompatibilityValidator = "org.jetbrains.kotlinx:binary-compatibility-validator:${Versions.binaryCompatibilityValidator}"
    const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.detekt}"
    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
    const val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val ktlintGradle = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.ktlintGradle}"
    const val nexusPublish = "io.github.gradle-nexus:publish-plugin:${Versions.nexusPublish}"
    const val shadow = "com.github.johnrengelman:shadow:${Versions.shadow}"
}

dependencies {
    implementation(Plugins.detekt)
    implementation(Plugins.dokka)
    implementation(Plugins.kotlinGradle)
    implementation(Plugins.ktlintGradle)
    implementation(Plugins.nexusPublish)
    implementation(Plugins.binaryCompatibilityValidator)
    implementation(Plugins.shadow)
}

allprojects {
    group = rootProject.properties["group"] as String
    version = rootProject.properties["version"] as String
}
