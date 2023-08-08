package org.partiql.coverage.api.impl

internal object ReportKey {
    const val BRANCH_COUNT: String = "\$pql-bc"
    const val BRANCH_CONDITION_COUNT: String = "\$pql-bcc"
    const val PACKAGE_NAME: String = "\$pql-pan"
    const val PROVIDER_NAME: String = "\$pql-prn"
    const val ORIGINAL_STATEMENT: String = "\$pql-os"
    const val LINE_NUMBER_OF_TARGET_PREFIX: String = "\$pql-lft"
    const val TYPE_OF_TARGET_PREFIX: String = "\$pql-tft"
    const val COVERAGE_TARGET_PREFIX: String = "\$pql-ct"
    const val OUTCOME_OF_TARGET_PREFIX: String = "\$pql-oft"
    const val TARGET_COUNT_PREFIX: String = "\$pql-rob"
    const val DELIMITER: String = "::"

    enum class CoverageTarget {
        BRANCH,
        BRANCH_CONDITION
    }
}