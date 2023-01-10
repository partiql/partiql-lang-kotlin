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
    id(Plugins.application)
}

dependencies {
    implementation(Deps.dotlin)
    implementation(Deps.ionElement)
    implementation(Deps.kasechange)
    implementation(Deps.kotlinPoet)
    implementation(Deps.picoCli)
}

application {
    applicationName = "sprout"
    mainClass.set("org.partiql.sprout.SproutKt")
}

distributions {
    main {
        distributionBaseName.set("sprout")
    }
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}
