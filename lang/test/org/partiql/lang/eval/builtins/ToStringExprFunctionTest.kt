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

class ToStringExprFunctionTest : EvaluatorTestBase() {
    // Note that the amount of testing here is a bit on the light side because most of the testing for the formatting
    // functionality behind `to_string` is in TimestampTemporalAccessorTests.
    @Test fun to_string1() = assertEval("to_string(`0500-03-09`, 'MM/dd/yyyy')", "\"03/09/0500\"")
    @Test fun to_string2() = assertEval("to_string(`0500-03-09`, 'M/d/y')", "\"3/9/500\"")
    @Test fun to_string3() = assertEval("to_string(`0001-03-09`, 'MM/dd/yyyy')", "\"03/09/0001\"")
    @Test fun to_string4() = assertEval("to_string(`0001-03-09`, 'M/d/y')", "\"3/9/1\"")
    @Test fun to_string5() = assertEval("to_string(`9999-03-09`, 'MM/dd/yyyy')", "\"03/09/9999\"")
    @Test fun to_string6() = assertEval("to_string(`9999-03-09`, 'M/d/y')", "\"3/9/9999\"")
    @Test fun to_string7() = assertEval("to_string(`0001-03-09`, 'y')", "\"1\"")
    @Test fun to_string8() = assertEval("to_string(`9999-03-09`, null)", "null")
    @Test fun to_string9() = assertEval("to_string(null, 'M/d/y')", "null")
    @Test fun to_string10() = assertEval("to_string(`9999-03-09`, missing)", "null")
    @Test fun to_string11() = assertEval("to_string(missing, 'M/d/y')", "null")

    @Test fun to_string_common_1() = assertEval("to_string(`1969-07-20T20:18Z`, 'MMMM d, y')", "\"July 20, 1969\"")
    @Test fun to_string_common_2() = assertEval("to_string(`1969-07-20T20:18Z`, 'MMM d, yyyy')", "\"Jul 20, 1969\"")
    @Test fun to_string_common_3() = assertEval("to_string(`1969-07-20T20:18Z`, 'M-d-yy')", "\"7-20-69\"")
    @Test fun to_string_common_4() = assertEval("to_string(`1969-07-20T20:18Z`, 'MM-d-y')", "\"07-20-1969\"")
    @Test fun to_string_common_5() = assertEval("to_string(`1969-07-20T20:18Z`, 'MMMM d, y h:m a')", "\"July 20, 1969 8:18 PM\"")
    @Test fun to_string_common_6() = assertEval("to_string(`1969-07-20T20:18Z`, 'y-MM-dd''T''H:m:ssX')", "\"1969-07-20T20:18:00Z\"")
    @Test fun to_string_common_7() = assertEval("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssX')", "\"1969-07-20T20:18:00+08\"")
    @Test fun to_string_common_8() = assertEval("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXX')", "\"1969-07-20T20:18:00+0800\"")
    @Test fun to_string_common_9() = assertEval("to_string(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXXX')", "\"1969-07-20T20:18:00+08:00\"")


    @Test
    fun to_string_invalid_symbol1() {
        checkInputThrowingEvaluationException(
            "to_string(`2017-01-01`, 'b')", //Symbol 'b' is unknown to Java's DateTimeFormatter
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "b"))
    }

    @Test
    fun to_string_invalid_symbol2() {
        checkInputThrowingEvaluationException(
            //Symbol 'z' is known to Java's DateTimeFormatter but is not handled by TimestampTemporalAccessor
            "to_string(`2017-01-01`, 'Y')",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "Y"))
    }

    @Test
    fun to_string_invalid_symbol3() {
        checkInputThrowingEvaluationException(
            //Symbol 'VV' is known to Java's DateTimeFormatter but is not handled by TimestampTemporalAccessor
            //*and* causes a different exception to be thrown by DateTimeFormatter.format() than 'z'
            "to_string(`2017-01-01`, 'VV')",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
            mapOf(Property.LINE_NUMBER to 1L,
                  Property.COLUMN_NUMBER to 1L,
                  Property.TIMESTAMP_FORMAT_PATTERN to "VV"))
    }

}