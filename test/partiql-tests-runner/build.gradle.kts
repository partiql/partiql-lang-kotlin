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
    testImplementation(project(":test:partiql-lang-import", configuration = "shadow"))
    testImplementation(project(":partiql-eval"))
    testImplementation(project(":partiql-parser", configuration = "shadow"))
    testImplementation(project(":partiql-planner"))
    testImplementation(project(":plugins:partiql-memory"))
}

val tests = System.getenv()["PARTIQL_TESTS_DATA"] ?: "../partiql-tests/partiql-tests-data"
val reportDir = file("$buildDir/conformance-test-report").absolutePath

object Env {
    const val PARTIQL_EVAL = "PARTIQL_EVAL_TESTS_DATA"
    const val PARTIQL_EQUIV = "PARTIQL_EVAL_EQUIV_TESTS_DATA"
}

fun setEnvironmentDataDirectories(test: Test) {
    // Set PartiQL Evaluation Test Directory
    val conformanceDataEval = file("$tests/eval/").absolutePath
    val projectDataEval = file("$buildDir/resources/test/ported/eval/").absolutePath
    test.environment(Env.PARTIQL_EVAL, "$conformanceDataEval:$projectDataEval")
    // Set PartiQL Evaluation Equivalence Test Directory
    val conformanceDataEquiv = file("$tests/eval-equiv/").absolutePath
    val projectDataEquiv = file("$buildDir/resources/test/ported/eval-equiv/").absolutePath
    test.environment(Env.PARTIQL_EQUIV, "$conformanceDataEquiv:$projectDataEquiv")
}

tasks.test {
    useJUnitPlatform()
    setEnvironmentDataDirectories(this)

    // To make it possible to run ConformanceTestReport in unit test UI runner, comment out this check:
    // exclude("org/partiql/runner/ConformanceTestEval.class", "org/partiql/runner/ConformanceTestLegacy.class")

    // May 2023: Disabled conformance testing during regular project build, because fail lists are out of date.
    // exclude("org/partiql/runner/ConformanceTest.class")
}

val createReportDir by tasks.registering {
    if (File(reportDir).exists()) {
        delete(File(reportDir))
    }
    mkdir(reportDir)
}

val generateTestReport by tasks.registering(Test::class) {
    dependsOn(createReportDir)
    useJUnitPlatform()
    setEnvironmentDataDirectories(this)
    environment("conformanceReportDir", reportDir)
    include("org/partiql/runner/ConformanceTestEval.class", "org/partiql/runner/ConformanceTestLegacy.class")
    if (project.hasProperty("engine")) {
        val engine = project.property("engine")!! as String
        if (engine.toLowerCase() == "legacy") {
            exclude("org/partiql/runner/ConformanceTestEval.class")
        } else if (engine.toLowerCase() == "eval") {
            exclude("org/partiql/runner/ConformanceTestLegacy.class")
        } else {
            throw InvalidUserDataException("Expect engine property to be either Legacy or Eval, received $engine")
        }
    }
}
