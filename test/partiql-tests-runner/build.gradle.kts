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
    id(Plugins.library)
    application
}

application {
    mainClass.set("org.partiql.runner.ConformanceComparisonKt")
}

dependencies {
    implementation(Deps.ionElement)
    testImplementation(project(":lang"))
}

val tests = "../partiql-tests/partiql-tests-data"

object Env {
    const val PARTIQL_EVAL = "PARTIQL_EVAL_TESTS_DATA"
    const val PARTIQL_EQUIV = "PARTIQL_EVAL_EQUIV_TESTS_DATA"
}

tasks.test {
    useJUnitPlatform()
    environment(Env.PARTIQL_EVAL, file("$tests/eval/").absolutePath)
    environment(Env.PARTIQL_EQUIV, file("$tests/eval-equiv/").absolutePath)

    if (!project.hasProperty("conformanceReport")) {
        exclude("org/partiql/runner/TestRunner\$ConformanceTestsReportRunner.class")
    }
}
