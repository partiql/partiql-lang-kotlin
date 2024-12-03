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
}

dependencies {
    api(project(":partiql-spi"))
    api(project(":partiql-types"))
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
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

publish {
    artifactId = "partiql-catalog"
    name = "PartiQL Catalog"
    description = "Contains all of the PartiQL System's functions and operators."
}
