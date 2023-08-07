/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

/**
 * Represents the static structure of a compiled [Expression]. This structure is distinct from the execution data found
 * within [CoverageData].
 *
 * @param branches represents the distinct outcome of an expression/clause that dictates control flow. For example,
 * a WHERE clause has two potential branches/outcomes.
 */
public data class CoverageStructure(
    val branches: Map<String, Branch> = emptyMap(),
    val branchConditions: Map<String, BranchCondition> = emptyMap()
) {
    public data class Branch(
        val id: String,
        val type: Type,
        val outcome: Outcome,
        val line: Long
    ) {
        public enum class Type {
            WHERE,
            HAVING,
            CASE_WHEN
        }

        public enum class Outcome {
            TRUE,
            FALSE
        }
    }

    public data class BranchCondition(
        val id: String,
        val type: Type,
        val outcome: Outcome,
        val line: Long
    ) {
        public enum class Type {
            GT,
            GTE,
            LT,
            LTE,
            EQ,
            NEQ,
            AND,
            OR,
            NOT,
            BETWEEN,
            LIKE,
            IS,
            IN,
            CAN_CAST,
            CAN_LOSSLESS_CAST
        }
        public enum class Outcome {
            TRUE,
            FALSE,
            NULL,
            MISSING
        }
    }
}
