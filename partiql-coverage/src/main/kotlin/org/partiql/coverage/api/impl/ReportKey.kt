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
