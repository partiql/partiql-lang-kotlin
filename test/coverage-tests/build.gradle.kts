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

tasks.test {
    systemProperty("partiql.coverage.lcov.branch.enabled", true)
    systemProperty("partiql.coverage.lcov.branch.report.path", "$buildDir/partiql/coverage/branch/lcov.info")
    systemProperty("partiql.coverage.lcov.branch.html.enabled", true)
    systemProperty("partiql.coverage.lcov.branch.html.dir", "$buildDir/reports/partiql/branch/test")
    systemProperty("partiql.coverage.lcov.branch.threshold.min", 0.2)

    systemProperty("partiql.coverage.lcov.condition.enabled", true)
    systemProperty("partiql.coverage.lcov.condition.report.path", "$buildDir/partiql/coverage/condition/lcov.info")
    systemProperty("partiql.coverage.lcov.condition.html.enabled", true)
    systemProperty("partiql.coverage.lcov.condition.html.dir", "$buildDir/reports/partiql/condition/test")
    systemProperty("partiql.coverage.lcov.condition.threshold.min", 0.2)
}
