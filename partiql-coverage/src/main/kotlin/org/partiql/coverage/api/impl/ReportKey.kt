package org.partiql.coverage.api.impl

public object ReportKey {
    public const val BRANCH_COUNT: String = "\$pql-bc"
    public const val CONDITION_COUNT: String = "\$pql-cc"
    public const val PACKAGE_NAME: String = "\$pql-pan"
    public const val PROVIDER_NAME: String = "\$pql-prn"
    public const val ORIGINAL_STATEMENT: String = "\$pql-os"
    public const val LINE_NUMBER_OF_CONDITION_PREFIX: String = "\$pql-lfc_"
    public const val LINE_NUMBER_OF_BRANCH_PREFIX: String = "\$pql-lfb_"
    public const val RESULT_OF_CONDITION_PREFIX: String = "\$pql-roc_"
    public const val RESULT_OF_BRANCH_PREFIX: String = "\$pql-rob_"
}