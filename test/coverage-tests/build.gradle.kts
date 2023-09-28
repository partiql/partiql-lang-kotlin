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
}

dependencies {
    testImplementation(project(":partiql-coverage"))
}

// Placement of Coverage Report/HTML
val branchReportPath = buildDir.resolve("partiql").resolve("coverage").resolve("branch").resolve("lcov.info")
val branchHtmlDir = buildDir.resolve("reports").resolve("partiql").resolve("branch").resolve("test")
val conditionReportPath = buildDir.resolve("partiql").resolve("coverage").resolve("condition").resolve("lcov.info")
val conditionHtmlDir = buildDir.resolve("reports").resolve("partiql").resolve("condition").resolve("test")

// Other files to check
val nestedPackageDir = buildDir.resolve("reports").resolve("partiql").resolve("condition").resolve("test")
    .resolve("html").resolve("org").resolve("partiql").resolve("test").resolve("coverage").resolve("nested")

// Checks that the Report & HTML Dir are generated.
val coverageReportCheck = tasks.register("coverageReportCheck") {
    doLast {
        if (branchReportPath.exists().not()) { throw Exception("$branchReportPath does not exist.") }
        if (conditionReportPath.exists().not()) { throw Exception("$conditionReportPath does not exist.") }
        if (branchHtmlDir.exists().not()) { throw Exception("$branchHtmlDir does not exist.") }
        if (conditionHtmlDir.exists().not()) { throw Exception("$conditionHtmlDir does not exist.") }
        if (nestedPackageDir.exists().not()) { throw Exception("$nestedPackageDir does not exist.") }
    }
}

tasks.test {
    // Branch Configurations
    systemProperty("partiql.coverage.lcov.branch.enabled", true)
    systemProperty("partiql.coverage.lcov.branch.report.path", branchReportPath)
    systemProperty("partiql.coverage.lcov.branch.html.dir", branchHtmlDir)
    systemProperty("partiql.coverage.lcov.branch.threshold.min", 0.2)

    // Branch Condition Configurations
    systemProperty("partiql.coverage.lcov.branch-condition.enabled", true)
    systemProperty("partiql.coverage.lcov.branch-condition.report.path", conditionReportPath)
    systemProperty("partiql.coverage.lcov.branch-condition.html.dir", conditionHtmlDir)
    systemProperty("partiql.coverage.lcov.branch-condition.threshold.min", 0.2)

    // Checks that the Report & HTML Dir are generated.
    finalizedBy(coverageReportCheck)
}
