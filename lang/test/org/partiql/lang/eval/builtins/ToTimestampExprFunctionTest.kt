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
 * Note that tests here on on the light side because most of the testing is done in [TimestampParserTest].
 */
class ToTimestampExprFunctionTest : EvaluatorTestBase() {

    @Test fun to_timestamp_common_1() = assertEval("to_timestamp('July 20, 1969', 'MMMM d, y')", "1969-07-20T")
    @Test fun to_timestamp_common_2() = assertEval("to_timestamp('Jul 20, 1969', 'MMM d, yyyy')", "1969-07-20T")
    @Test fun to_timestamp_common_4() = assertEval("to_timestamp('1969-07-20T20:18Z', 'yyyy-MM-dd''T''HH:mmX')", "1969-07-20T20:18Z")
    @Test fun to_timestamp_common_5() = assertEval("to_timestamp('July 20, 1969 8:18 PM', 'MMMM d, y h:m a')", "1969-07-20T20:18-00:00")
    @Test fun to_timestamp_common_6() = assertEval("to_timestamp('1969-07-20T20:18:00Z', 'yyyy-MM-dd''T''H:m:ssX')", "1969-07-20T20:18:00Z")
    @Test fun to_timestamp_common_7() = assertEval("to_timestamp('1969-07-20T20:18:01+08', 'yyyy-MM-dd''T''H:m:ssX')", "1969-07-20T20:18:01+08:00")
    @Test fun to_timestamp_common_8() = assertEval("to_timestamp('1969-07-20T20:18:02+0800', 'yyyy-MM-dd''T''H:m:ssXXXX')", "1969-07-20T20:18:02+08:00")
    @Test fun to_timestamp_common_9() = assertEval("to_timestamp('1969-07-20T20:18:03+08:00', 'yyyy-MM-dd''T''H:m:ssXXXXX')", "1969-07-20T20:18:03+08:00")

    @Test fun to_timestamp_parse_timestamp_1() = assertEval("to_timestamp('1969-07-20T20:18:00Z')", "1969-07-20T20:18:00Z")
    @Test fun to_timestamp_parse_timestamp_2() = assertEval("to_timestamp('1969-07-20T20:18:03+08:00')", "1969-07-20T20:18:03+08:00")

    @Test fun to_timestamp_null_arg_1() = assertEval("to_timestamp(null)", "null")
    @Test fun to_timestamp_null_arg_2() = assertEval("to_timestamp(null, 'M-d-yyyy')", "null")
    @Test fun to_timestamp_null_arg_3() = assertEval("to_timestamp('07-20-1969', null)", "null")
    @Test fun to_timestamp_null_arg_4() = assertEval("to_timestamp(null, null)", "null")

    @Test fun to_timestamp_missing_arg_1() = assertEval("to_timestamp(missing)", "null")
    @Test fun to_timestamp_missing_arg_2() = assertEval("to_timestamp(missing, 'M-d-yyyy')", "null")
    @Test fun to_timestamp_missing_arg_3() = assertEval("to_timestamp('07-20-1969', missing)", "null")
    @Test fun to_timestamp_missing_arg_4() = assertEval("to_timestamp(null, null)", "null")

    @Test
    fun to_timestamp_too_few_args() {
        checkInputThrowingEvaluationException(
            "to_timestamp()",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 1,
                  Property.EXPECTED_ARITY_MAX to 2))
    }

    @Test
    fun to_timestamp_too_many_args() {
        checkInputThrowingEvaluationException(
            "to_timestamp('one', 'two', 'three')",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.EXPECTED_ARITY_MIN to 1,
                  Property.EXPECTED_ARITY_MAX to 2))
    }

    @Test
    fun to_timestamp_invalid_ion_timestamp() {
        checkInputThrowingEvaluationException(
            "to_timestamp('not a valid timestamp')",
            ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L))
    }

    @Test
    fun to_timestamp_empty_format_pattern() {
        checkInputThrowingEvaluationException(
            "to_timestamp('doesnt matter', '')",
            ErrorCode.EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "",
                  Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to "YEAR"))
   }

    @Test
    fun to_timestamp_invalid_format_pattern() {
        checkInputThrowingEvaluationException(
            "to_timestamp('doesnt matter', 'asdfasdfasdf')",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "asdfasdfasdf"))
   }

    @Test
    fun to_timestamp_invalid_timestamp() {
        checkInputThrowingEvaluationException(
            "to_timestamp('asdf', 'yyyy')",
            ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "yyyy"))
   }
}