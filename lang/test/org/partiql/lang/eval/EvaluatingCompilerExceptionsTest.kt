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

package org.partiql.lang.eval

import org.partiql.lang.errors.*
import org.partiql.lang.util.*
import org.junit.*

class EvaluatingCompilerExceptionsTest : EvaluatorTestBase() {

    @Test
    fun notOnOne() = assertThrows("Expected boolean: 1", NodeMetadata(1, 1)) {
        voidEval("not 1")
    }

    @Test
    fun betweenIncompatiblePredicate() = assertThrows("Cannot compare values: 'APPLE', 2", NodeMetadata(4, 19)) {
        voidEval("""
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x BETWEEN 'A' AND 2
        """)
    }

    @Test
    fun notBetweenIncompatiblePredicate() = assertThrows("Cannot compare values: 'APPLE', 1", NodeMetadata(4, 19)) {
        voidEval("""
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x NOT BETWEEN 1 AND 'Y'
        """)
    }

    @Test
    fun shadowedVariables() = assertThrows("Multiple matches were found for the specified identifier", NodeMetadata(1, 14)) {
        voidEval("""SELECT VALUE a FROM `[{v:5}]` AS item, @item.v AS a, @item.v AS a""")
    }

    @Test
    fun topLevelCountStar() = assertThrows("COUNT(*) is not allowed in this context",
                                           NodeMetadata(1, 1)) { voidEval("""COUNT(*)""") }


    @Test
    fun selectValueCountStar() = assertThrows("COUNT(*) is not allowed in this context", NodeMetadata(1, 14)) {
        voidEval("""SELECT VALUE COUNT(*) FROM numbers""")
    }

    @Test
    fun selectListNestedAggregateCall() = assertThrows("The arguments of an aggregate function cannot contain aggregate functions", NodeMetadata(1, 12)) {
        voidEval("""SELECT SUM(AVG(n)) FROM <<numbers, numbers>> AS n""")
    }

    private val BAD_ALIAS_SQL = "SELECT VALUE y FROM << 'el1' >> AS x"
    @Test fun badAlias() {
        //Note that the current default for CompileOptions.undefinedVariable is UndefinedVariableBehavior.ERROR
        checkInputThrowingEvaluationException(
            BAD_ALIAS_SQL,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.BINDING_NAME to "y"))
    }

    @Test fun missingAlias() =
        //Same query as previous test--but DO NOT throw exception this time because of UndefinedVariableBehavior.MISSING
        assertEval(BAD_ALIAS_SQL, "[null]",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) })

    @Test
    fun wrongArityExists() = assertThrows("Expected a single argument for exists but found: 0", NodeMetadata(1, 1)) {
        voidEval("exists()")
    }

    @Test
    fun unknownFunction() = assertThrows("No such function: unknownfunction", NodeMetadata(1, 1)) {
        voidEval("unknownFunction()")
    }

    @Test
    fun rightJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a RIGHT CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id")
    }

    @Test
    fun outerJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a OUTER CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id")
    }

    @Test
    fun substringWrongTypeSecondArgument() = assertThrows("Argument 2 of substring was not INT.", NodeMetadata(1, 1)) {
        voidEval("substring('abcdefghi' from '1')")
    }

    @Test
    fun substringWrongTypeThirdArgument() = assertThrows("Argument 3 of substring was not INT.", NodeMetadata(1, 1)) {
        voidEval("substring('abcdefghi' from 1 for '1')")
    }

    @Test
    fun addingWrongTypes() = assertThrows("Expected number: \"a\"", NodeMetadata(1, 11)) {
        voidEval("1 + 2 + 4 + 'a' + 5")
    }

    @Test
    fun badCastToInt() = checkInputThrowingEvaluationException(
        "CAST('a' as int) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 5) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "INTEGER"))

    @Test
    fun badCastInSelectToInt() = checkInputThrowingEvaluationException(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE CAST(_2 as INT) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 79) + mapOf(Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INTEGER"))

    @Test
    fun badCastToDecimal() = checkInputThrowingEvaluationException(
        "CAST('a' as DECIMAL) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 5) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "DECIMAL"),
        NumberFormatException::class)

    @Test
    fun badCastToTimestamp() = checkInputThrowingEvaluationException(
        "CAST('2010-01-01T10' as TIMESTAMP) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 5) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "TIMESTAMP"),
        IllegalArgumentException::class)

    @Test
    fun divideByZero() = checkInputThrowingEvaluationException(
        "1 / 0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        sourceLocationProperties(1, 3)) 

    @Test
    fun divideByZeroDecimal() = checkInputThrowingEvaluationException(
        "1.0 / 0.0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        sourceLocationProperties(1, 5))

    @Test
    fun moduloByZero() = checkInputThrowingEvaluationException(
        "1 % 0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        sourceLocationProperties(1, 3))

    @Test
    fun moduloByZeroDecimal() = checkInputThrowingEvaluationException(
        "1.0 % 0.0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        sourceLocationProperties(1, 5))

    @Test
    fun divideByZeroInSelect() = assertThrows("/ by zero", NodeMetadata(1, 76)) {
        voidEval("SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 2}, {_1: a, _2: 3}]` WHERE _2 / 0 > 0")
    }

    @Test
    fun utcnowWithArgument() = assertThrows("utcnow() takes no arguments", NodeMetadata(1, 1)) {
        voidEval("utcnow(1)")
    }

    @Test
    fun ambiguousFieldOnStructCaseSensitiveLookup() = checkInputThrowingEvaluationException(
        """ select "repeated" from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        sourceLocationProperties(1, 9) + mapOf(Property.BINDING_NAME to "repeated", Property.BINDING_NAME_MATCHES to "repeated, repeated"))

    @Test
    fun ambiguousFieldOnStructCaseInsensitiveLookup() = checkInputThrowingEvaluationException(
        """ select REPEATED from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        sourceLocationProperties(1, 9) + mapOf(Property.BINDING_NAME to "REPEATED", Property.BINDING_NAME_MATCHES to "repeated, repeated"))

    @Test
    fun invalidEscapeSequenceInLike() = checkInputThrowingEvaluationException(
        """ '' like '^1' escape '^' """,
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        sourceLocationProperties(1, 10) + mapOf(Property.LIKE_ESCAPE to "^", Property.LIKE_PATTERN to "^1"))

    @Test
    fun unboundParameters() = checkInputThrowingEvaluationException(
        """SELECT ? FROM <<1>>""",
        ErrorCode.EVALUATOR_UNBOUND_PARAMETER,
        sourceLocationProperties(1, 8) + mapOf(Property.EXPECTED_PARAMETER_ORDINAL to 1, Property.BOUND_PARAMETER_COUNT to 0))

    @Test
    fun trimSpecKeywordBothNotUsedInTrim() =
        checkInputThrowingEvaluationException(
            "SELECT 1 FROM both",
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.BINDING_NAME to "both")
        )

    @Test
    fun trimSpecKeywordLeadingNotUsedInTrim() =
        checkInputThrowingEvaluationException(
            "SELECT 1 FROM leading",
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.BINDING_NAME to "leading")
        )

    @Test
    fun trimSpecKeywordTrailingNotUsedInTrim() =
        checkInputThrowingEvaluationException(
            "SELECT 1 FROM trailing",
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.BINDING_NAME to "trailing")
        )

    @Test
    fun trimSpecKeywordLeadingUsedAsSecondArgInTrim() =
        checkInputThrowingEvaluationException(
            "trim(both leading from 'foo')",
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.BINDING_NAME to "leading")
        )
}
