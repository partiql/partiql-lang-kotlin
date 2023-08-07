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
}

dependencies {
    api(project(":partiql-lang"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    implementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    implementation("org.junit.platform:junit-platform-launcher:1.9.3")
    implementation("org.jacoco:org.jacoco.report:0.8.10")
    implementation("com.googlecode.jgenhtml:jgenhtml:1.6")
    implementation(kotlin("reflect"))
}

publish {
    artifactId = "partiql-coverage"
    name = "PartiQL Code Coverage"
    description = "Code Coverage APIs for testing PartiQL source."
}
