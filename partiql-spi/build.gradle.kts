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
    implementation(project(":partiql-types"))
    implementation(Deps.ionElement)
    testImplementation(Deps.gson)
}

publish {
    artifactId = "partiql-spi"
    name = "PartiQL SPI"
    description = "Pluggable interfaces to allow for custom logic within the PartiQL library."
}

configurations {
    create("test")
}

tasks.register<Jar>("testArchive") {
    archiveBaseName.set("${project.name}-test")
    from(project.the<SourceSetContainer>()["test"].output)
    from(project.the<SourceSetContainer>()["test"].allSource)
}

artifacts {
    add("test", tasks["testArchive"])
}
