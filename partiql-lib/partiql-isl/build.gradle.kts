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
//
plugins {
    id(Plugins.conventions)
    id(Plugins.dokka)
    id(Plugins.library)
    id(Plugins.pig)
    id(Plugins.publish)
}

dependencies {
    api(Deps.ionElement)
    api(Deps.ionJava)
    api(Deps.ionSchema)
    api(Deps.pigRuntime)
}

publish {
    artifactId = "partiql-isl-kotlin"
    name = "PartiQL ISL Kotlin"
    description = "An object model that allows for programmatic manipulation of Ion Schema Language schemas."
    url = "https://github.com/partiql/partiql-lang-kotlin/tree/main/lib/partiql-isl"
}

pig {
    namespace = "org.partiql.ionschema.model"
}

tasks.dokkaHtml {
    dependsOn(tasks.withType(org.partiql.pig.gradle.PigTask::class))
}
