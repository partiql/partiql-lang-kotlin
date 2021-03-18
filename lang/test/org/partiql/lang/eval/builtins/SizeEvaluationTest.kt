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

import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.junit.*

/**
 * More detailed tests are in [SizeExprFunctionTest]
 */
class SizeEvaluationTest : EvaluatorTestBase() {

    @Test
    fun emptyStruct() = assertEval("size({})", "0")

    @Test
    fun emptyList() = assertEval("size([])", "0")

    @Test
    fun emptySexp() = assertEval("size(sexp())", "0")

    @Test
    fun emptyBag() = assertEval("size(<<>>)", "0")

    @Test
    fun nonEmptyStruct() = assertEval("size(`{ a: 1 }`)", "1")

    @Test
    fun nonEmptyList() = assertEval("size(['foo'])", "1")

    @Test
    fun nonEmptySexp() = assertEval("size(sexp(1, 2, 3))", "3")

    @Test
    fun nonEmptyBag() = assertEval("size(<<'foo'>>)", "1")

    @Test
    fun nullArgument() = assertEval("size(null)", "null")

    @Test
    fun missingArgument() = assertEval("size(missing)", "null")


    @Test
    fun lessArguments() = checkInputThrowingEvaluationException("size()",
                                                                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                      Property.EXPECTED_ARITY_MAX to 1,
                                                                      Property.LINE_NUMBER to 1L,
                                                                      Property.COLUMN_NUMBER to 1L))

    @Test
    fun moreArguments() = checkInputThrowingEvaluationException("size(null, null)",
                                                                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                      Property.EXPECTED_ARITY_MAX to 1,
                                                                      Property.LINE_NUMBER to 1L,
                                                                      Property.COLUMN_NUMBER to 1L))

    @Test
    fun wrongTypeOfArgument() = checkInputThrowingEvaluationException("size(1)",
                                                                      ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                      mapOf(Property.EXPECTED_ARGUMENT_TYPES to "LIST or BAG or STRUCT",
                                                                            Property.ACTUAL_ARGUMENT_TYPES to "INT",
                                                                            Property.FUNCTION_NAME to "size",
                                                                            Property.LINE_NUMBER to 1L,
                                                                            Property.COLUMN_NUMBER to 1L))
}