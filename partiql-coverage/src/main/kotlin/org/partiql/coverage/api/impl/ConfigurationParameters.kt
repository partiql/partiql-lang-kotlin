package org.partiql.coverage.api.impl

internal enum class ConfigurationParameters(val key: String, val default: Any?) {
    BRANCH_MINIMUM("org.partiql.coverage.config.branch-minimum", null),
    OUTPUT_HTML_DIR("org.partiql.coverage.config.html-output-dir", "build/reports/partiql/test/html"),
    REPORT_LOCATION("org.partiql.coverage.config.report-location", "build/partiql/coverage/report/cov.info")
}