/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.lang.eval.builtins

import org.junit.*
import org.partiql.lang.errors.*
import org.partiql.lang.errors.ErrorCode.*
import org.partiql.lang.errors.Property.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.ExprValueType.*

class ConcatEvaluationTest : EvaluatorTestBase() {
    private val argumentTypeMap = mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRING, SYMBOL).toString())

    @Test
    fun concatWrongLeftType() = 
        checkInputThrowingEvaluationException("1 || 'a'",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 3) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(INT, STRING).toString()))

    @Test
    fun concatWrongRightType() =
        checkInputThrowingEvaluationException("'a' || 1",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 5) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRING, INT).toString()))

    @Test
    fun concatWrongBothTypes() =
        checkInputThrowingEvaluationException("{} || `2010T`",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 4) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRUCT, TIMESTAMP).toString()))

    @Test
    fun strings() = assertEval("'a' || 'b'", "\"ab\"")

    @Test
    fun symbols() = assertEval("`'a'` || `'b'`", "\"ab\"")

    @Test
    fun stringAndSymbols() = assertEval("'a' || `'b'`", "\"ab\"")

    @Test
    fun nullAndString() = assertEval("null || 'b'", "null")

    @Test
    fun missingAndString() = assertEval("missing || 'b'", "null")
}
