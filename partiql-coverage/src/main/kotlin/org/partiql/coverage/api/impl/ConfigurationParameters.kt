package org.partiql.coverage.api.impl

/**
 * Defines the System Properties (JUnit5) to be configured for PartiQL Code Coverage.
 */
internal enum class ConfigurationParameters(val key: String) {
    BRANCH_MINIMUM("partiql.coverage.lcov.threshold.branch.min"),
    CONDITION_MINIMUM("partiql.coverage.lcov.threshold.condition.min"),
    LCOV_ENABLED("partiql.coverage.lcov.enabled"),
    LCOV_REPORT_LOCATION("partiql.coverage.lcov.path"),
    LCOV_HTML_ENABLED("partiql.coverage.lcov.html.enabled"),
    LCOV_HTML_OUTPUT_DIR("partiql.coverage.lcov.html.dir")
}
