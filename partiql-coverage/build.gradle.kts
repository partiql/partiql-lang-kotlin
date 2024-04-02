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
    id(Plugins.publish)
    // Use https://github.com/Kotlin/binary-compatibility-validator to maintain list of public binary APIs (defaults
    // to <project dir>/api/<project dir>.api). When changes are made to public APIs (e.g. modifying a public class,
    // adding a public function, etc.), the gradle `apiCheck` task will fail. To fix this error, run the `apiDump` task
    // to update these .api files and commit the changes.
    // See https://github.com/Kotlin/binary-compatibility-validator#optional-parameters for additional configuration.
    id(Plugins.binaryCompatibilityValidator) version Versions.binaryCompatibilityValidator
}

dependencies {
    api(project(":partiql-lang"))
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

publish {
    artifactId = "partiql-coverage"
    name = "PartiQL Code Coverage"
    description = "Code Coverage APIs for testing PartiQL source."
}
