package org.partiql.coverage.api.impl

/**
 * Defines the System Properties (JUnit5) to be configured for PartiQL Code Coverage.
 */
internal enum class ConfigurationParameter(val key: String) {
    LCOV_BRANCH_ENABLED("partiql.coverage.lcov.branch.enabled"),
    LCOV_BRANCH_HTML_DIR("partiql.coverage.lcov.branch.html.dir"),
    LCOV_BRANCH_MINIMUM("partiql.coverage.lcov.branch.threshold.min"),
    LCOV_BRANCH_REPORT_LOCATION("partiql.coverage.lcov.branch.report.path"),
    LCOV_CONDITION_ENABLED("partiql.coverage.lcov.condition.enabled"),
    LCOV_CONDITION_HTML_DIR("partiql.coverage.lcov.condition.html.dir"),
    LCOV_CONDITION_MINIMUM("partiql.coverage.lcov.condition.threshold.min"),
    LCOV_CONDITION_REPORT_LOCATION("partiql.coverage.lcov.condition.report.path"),
}
