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
    // Use https://github.com/Kotlin/binary-compatibility-validator to maintain list of public binary APIs (defaults
    // to <project dir>/api/<project dir>.api). When changes are made to public APIs (e.g. modifying a public class,
    // adding a public function, etc.), the gradle `apiCheck` task will fail. To fix this error, run the `apiDump` task
    // to update these .api files and commit the changes.
    // See https://github.com/Kotlin/binary-compatibility-validator#optional-parameters for additional configuration.
    id(Plugins.binaryCompatibilityValidator) version Versions.binaryCompatibilityValidator
}

apiValidation {
    /**
     * Set of annotations that exclude API from being public.
     * Typically, it is all kinds of `@InternalApi` annotations that mark
     * effectively private API that cannot be actually private for technical reasons.
     */
    nonPublicMarkers.addAll(listOf("org.partiql.value.PartiQLValueExperimental", "org.partiql.value.PartiQLTimestampExperimental"))
}

dependencies {
    implementation(Deps.ionElement)
    implementation(Deps.kotlinxCollections)
}

publish {
    artifactId = "partiql-types"
    name = "PartiQL Types"
    description = "The core PartiQL types."
}
