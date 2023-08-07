package org.partiql.coverage.api.impl

public object ReportKey {
    public const val BRANCH_COUNT: String = "\$pql-bc"
    public const val CONDITION_COUNT: String = "\$pql-cc"
    public const val PACKAGE_NAME: String = "\$pql-pan"
    public const val PROVIDER_NAME: String = "\$pql-prn"
    public const val ORIGINAL_STATEMENT: String = "\$pql-os"
    public const val LINE_NUMBER_OF_CONDITION_PREFIX: String = "\$pql-lfc"
    public const val LINE_NUMBER_OF_TARGET_PREFIX: String = "\$pql-lft"
    public const val TYPE_OF_TARGET_PREFIX: String = "\$pql-tft"
    public const val COVERAGE_TARGET_PREFIX: String = "\$pql-ct"
    public const val OUTCOME_OF_TARGET_PREFIX: String = "\$pql-oft"
    public const val RESULT_OF_CONDITION_PREFIX: String = "\$pql-roc"
    public const val TARGET_COUNT_PREFIX: String = "\$pql-rob"
    public const val DELIMITER: String = "::"

    public enum class CoverageTarget {
        BRANCH,
        BRANCH_CONDITION
    }
}