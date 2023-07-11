package org.partiql.coverage.api.impl

internal object ConfigurationParameters {
    /**
     * Defines a user's custom report location
     */
    internal const val REPORT_LOCATION_KEY = "org.partiql.coverage.config.report-location"

    /**
     * Default location for report
     */
    internal const val REPORT_LOCATION_DEFAULT_VALUE = "build/partiql/coverage/report/cov.info"

    internal const val BRANCH_MINIMUM_KEY = "org.partiql.coverage.config.branch-minimum"
}