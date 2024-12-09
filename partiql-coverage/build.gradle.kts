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
    id(Plugins.dokka)
    id(Plugins.library)
    // TODO: Once code coverage is supported with the new evaluator, we can publish a new version.
    // id(Plugins.publish)
}

dependencies {
    // TODO: Once code coverage is published again, we can re-add the HEAD of the PartiQL Library.
    api("org.partiql:partiql-lang-kotlin:0.14.8")
    implementation(Deps.junitApi)
    implementation(Deps.junitParams)
    implementation(Deps.junitPlatformLauncher)
    implementation(Deps.jgenhtml)
}

// Need to add this as we have both Java and Kotlin sources. Dokka already handles multi-language projects. If
// Javadoc is enabled, we end up overwriting index.html (causing compilation errors).
tasks.withType<Javadoc>() {
    enabled = false
}

// START OF COMMENTED OUT CODE
// TODO: This has all be commented out due to the *temporary* removal of the publish API

// tasks.shadowJar {
//     configurations = listOf(project.configurations.shadow.get())
// }

// Workaround for https://github.com/johnrengelman/shadow/issues/651
// components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
//     c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
//         skip()
//     }
// }
// publish {
//     artifactId = "partiql-coverage"
//     name = "PartiQL Code Coverage"
//     description = "Code Coverage APIs for testing PartiQL source."
// }
//  END OF COMMENTED OUT CODE
