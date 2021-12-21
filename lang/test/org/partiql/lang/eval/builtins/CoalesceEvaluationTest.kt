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

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase

class CoalesceEvaluationTest : EvaluatorTestBase() {
    @Test
    fun nullSingleArgument() = assertEval("coalesce(null)", "null")

    @Test fun nullMultipleArgument() = assertEval("coalesce(null, null)", "null")

    @Test fun missingSingleArgument() = assertEval("coalesce(missing)", "null")

    @Test fun missingMultipleArgument() = assertEval("coalesce(missing, missing)", "null")

    @Test fun singleArgument() = assertEval("coalesce(1)", "1")

    @Test fun nullAfterValue() = assertEval("coalesce(1, null)", "1")

    @Test fun valueAfterNull() = assertEval("coalesce(null, 2)", "2")

    @Test fun missingAfterValue() = assertEval("coalesce(1, missing)", "1")

    @Test fun valueAfterMissing() = assertEval("coalesce(missing, 2)", "2")

    @Test fun nullThenMissing() = assertEval("coalesce(null, missing)", "null")

    @Test fun missingThenNull() = assertEval("coalesce(missing, null)", "null")

    @Test fun severalArgumentsNullAndMissingMixed() = assertEval("coalesce(null, missing, null, null, 2, 3, 4, 5)", "2")

    @Test fun severalNullArguments() = assertEval("coalesce(null, null, 2, 3, 4, 5)", "2")

    @Test fun severalMissingArguments() = assertEval("coalesce(missing, missing, 2, 3, 4, 5)", "2")

    @Test
    fun zeroArguments() = checkInputThrowingEvaluationException(input = "coalesce()",
                                                                errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                expectErrorContextValues = sourceLocationProperties(1, 1) +
                                                                                           mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                                                 Property.EXPECTED_ARITY_MAX to Int.MAX_VALUE))
}