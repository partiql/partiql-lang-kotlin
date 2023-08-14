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

package org.partiql.coverage.api.impl

/**
 * Defines the System Properties (JUnit5) to be configured for PartiQL Code Coverage.
 */
internal enum class ConfigurationParameter(val key: String) {
    LCOV_BRANCH_ENABLED("partiql.coverage.lcov.branch.enabled"),
    LCOV_BRANCH_HTML_DIR("partiql.coverage.lcov.branch.html.dir"),
    LCOV_BRANCH_MINIMUM("partiql.coverage.lcov.branch.threshold.min"),
    LCOV_BRANCH_REPORT_LOCATION("partiql.coverage.lcov.branch.report.path"),
    LCOV_BRANCH_CONDITION_ENABLED("partiql.coverage.lcov.branch-condition.enabled"),
    LCOV_BRANCH_CONDITION_HTML_DIR("partiql.coverage.lcov.branch-condition.html.dir"),
    LCOV_BRANCH_CONDITION_MINIMUM("partiql.coverage.lcov.branch-condition.threshold.min"),
    LCOV_BRANCH_CONDITION_REPORT_LOCATION("partiql.coverage.lcov.branch-condition.report.path"),
}
