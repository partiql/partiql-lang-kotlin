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
    id(Plugins.testFixtures)
}

dependencies {
    api(Deps.ionElement)
    api(project(":partiql-types"))
    implementation(Deps.kotlinxCollections)
    testImplementation(Deps.kasechange)
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

// TODO: Once we move to only public Java APIs, we can use Javadoc.
// Need to add this as we have both Java and Kotlin sources. Dokka already handles multi-language projects. If
// Javadoc is enabled, we end up overwriting index.html (causing compilation errors).
tasks.withType<Javadoc> {
    enabled = false
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

tasks.compileTestFixturesKotlin {
    kotlinOptions.jvmTarget = Versions.jvmTarget
    kotlinOptions.apiVersion = Versions.kotlinApi
    kotlinOptions.languageVersion = Versions.kotlinLanguage
}

publish {
    artifactId = "partiql-spi"
    name = "PartiQL SPI"
    description = "Pluggable interfaces to allow for custom logic within the PartiQL library."
}
