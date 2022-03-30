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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.sourceLocationProperties
import org.partiql.lang.util.to

class EvaluatingCompilerExceptionsTest : EvaluatorTestBase() {

    // new tests in this file should be added here or parameterized like this with another @ParameterizedTest.
    // No time to refactor this entire test class to be parameterized but neither should we continue
    // to follow a pattern that we'd like to change anyway.
    // FIXME - these tests don't seem to work, and when enabled the options are set but the `FLOAT` type is missing
    //         the parameter at the point we test it in the EvaluatingCompiler
    @Disabled
    @ParameterizedTest
    @ArgumentsSource(ErrorTestCasesTestCases::class)
    fun errorTestCases(tc: EvaluatorErrorTestCase) =
        checkInputThrowingEvaluationException(tc, EvaluationSession.standard())
    class ErrorTestCasesTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            EvaluatorErrorTestCase(
                """CAST(12 AS FLOAT(1))""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                sourceLocationProperties(1, 13),
                compOptions = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS
            ),
            EvaluatorErrorTestCase(
                """CAN_CAST(12 AS FLOAT(1))""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                sourceLocationProperties(1, 17),
                compOptions = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS
            ),
            EvaluatorErrorTestCase(
                """12 IS FLOAT(1)""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                sourceLocationProperties(1, 8),
                compOptions = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS
            )
        )
    }

    @Test
    fun notOnOne() = assertThrows("not 1", "Expected boolean: 1", NodeMetadata(1, 1), "MISSING")

    @Test
    fun betweenIncompatiblePredicate() = assertThrows(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x BETWEEN 'A' AND 2
        """,
        "Cannot compare values: 'APPLE', 2",
        NodeMetadata(4, 19),
        "<<>>"
    )

    @Test
    fun notBetweenIncompatiblePredicate() = assertThrows(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x NOT BETWEEN 1 AND 'Y'
        """,
        "Cannot compare values: 'APPLE', 1",
        NodeMetadata(4, 19),
        "<<>>"
    )

    @Test
    fun shadowedVariables() = assertThrows(
        """SELECT VALUE a FROM `[{v:5}]` AS item, @item.v AS a, @item.v AS a""",
        "Multiple matches were found for the specified identifier",
        NodeMetadata(1, 14),
        "<<MISSING>>"
    )

    @Test
    fun topLevelCountStar() = assertThrows("""COUNT(*)""", "COUNT(*) is not allowed in this context", NodeMetadata(1, 1))

    @Test
    fun selectValueCountStar() = assertThrows(
        """SELECT VALUE COUNT(*) FROM numbers""",
        "COUNT(*) is not allowed in this context",
        NodeMetadata(1, 14)
    )

    @Test
    fun selectListNestedAggregateCall() = assertThrows(
        """SELECT SUM(AVG(n)) FROM <<numbers, numbers>> AS n""",
        "The arguments of an aggregate function cannot contain aggregate functions",
        NodeMetadata(1, 12)
    )

    private val sqlWithUndefinedVariable = "SELECT VALUE y FROM << 'el1' >> AS x"
    @Test
    fun badAlias() {
        // Note that the current default for CompileOptions.undefinedVariable is UndefinedVariableBehavior.ERROR
        checkInputThrowingEvaluationException(
            sqlWithUndefinedVariable,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.BINDING_NAME to "y"
            ),
            expectedPermissiveModeResult = "<<MISSING>>"
        )
    }

    @Test
    fun missingAlias() =
        // Same query as previous test--but DO NOT throw exception this time because of UndefinedVariableBehavior.MISSING
        assertEval(
            sqlWithUndefinedVariable, "[null]",
            compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) }
        )

    private val sqlWithUndefinedQuotedVariable = "SELECT VALUE \"y\" FROM << 'el1' >> AS x"
    @Test
    fun badQuotedAlias() {
        // Note that the current default for CompileOptions.undefinedVariable is UndefinedVariableBehavior.ERROR
        checkInputThrowingEvaluationException(
            sqlWithUndefinedQuotedVariable,
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.BINDING_NAME to "y"
            ),
            expectedPermissiveModeResult = "<<MISSING>>"
        )
    }

    @Test
    fun missingQuotedAlias() =
        // Same query as previous test--but DO NOT throw exception this time because of UndefinedVariableBehavior.MISSING
        assertEval(
            sqlWithUndefinedQuotedVariable, "[null]",
            compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) }
        )

    @Test
    fun wrongArityExists() = assertThrows("exists()", "exists takes a single argument, received: 0", NodeMetadata(1, 1))

    @Test
    fun unknownFunction() = assertThrows("unknownFunction()", "No such function: unknownfunction", NodeMetadata(1, 1))

    @Test
    fun rightJoin() = assertThrows(
        "SELECT * FROM animals AS a RIGHT CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id",
        "RIGHT and FULL JOIN not supported",
        NodeMetadata(1, 28)
    )

    @Test
    fun outerJoin() = assertThrows(
        "SELECT * FROM animals AS a OUTER CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id",
        "RIGHT and FULL JOIN not supported",
        NodeMetadata(1, 28)
    )

    @Test
    fun addingWrongTypes() = assertThrows("1 + 2 + 4 + 'a' + 5", "Expected number: \"a\"", NodeMetadata(1, 11), "MISSING")

    @Test
    fun badCastToInt() = checkInputThrowingEvaluationException(
        "CAST('a' as int) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 1) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "INT"),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun badCastInSelectToInt() = checkInputThrowingEvaluationException(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE CAST(_2 as INT) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 75) + mapOf(Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INT"),
        expectedPermissiveModeResult = "<<{'_1': `a`, '_2': 1}, {'_1': `a`, '_2': 3}>>"
    )

    @Test
    fun badCastToDecimal() = checkInputThrowingEvaluationException(
        "CAST('a' as DECIMAL) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 1) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "DECIMAL"),
        NumberFormatException::class,
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun badCastToTimestamp() = checkInputThrowingEvaluationException(
        "CAST('2010-01-01T10' as TIMESTAMP) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 1) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "TIMESTAMP"),
        IllegalArgumentException::class,
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun divideByZero() = checkInputThrowingEvaluationException(
        "1 / 0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        sourceLocationProperties(1, 3),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun divideByZeroDecimal() = checkInputThrowingEvaluationException(
        "1.0 / 0.0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        sourceLocationProperties(1, 5),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun moduloByZero() = checkInputThrowingEvaluationException(
        "1 % 0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        sourceLocationProperties(1, 3),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun moduloByZeroDecimal() = checkInputThrowingEvaluationException(
        "1.0 % 0.0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        sourceLocationProperties(1, 5),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun divideByZeroInSelect() = assertThrows(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 2}, {_1: a, _2: 3}]` WHERE _2 / 0 > 0",
        "/ by zero",
        NodeMetadata(1, 76),
        "<<>>"
    )

    @Test
    fun utcnowWithArgument() = assertThrows("utcnow(1)", "utcnow takes exactly 0 arguments, received: 1", NodeMetadata(1, 1))

    @Test
    fun ambiguousFieldOnStructCaseSensitiveLookup() = checkInputThrowingEvaluationException(
        """ select "repeated" from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        sourceLocationProperties(1, 9) + mapOf(Property.BINDING_NAME to "repeated", Property.BINDING_NAME_MATCHES to "repeated, repeated"),
        expectedPermissiveModeResult = "<<{}>>"
    )

    @Test
    fun ambiguousFieldOnStructCaseInsensitiveLookup() = checkInputThrowingEvaluationException(
        """ select REPEATED from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        sourceLocationProperties(1, 9) + mapOf(Property.BINDING_NAME to "REPEATED", Property.BINDING_NAME_MATCHES to "repeated, repeated"),
        expectedPermissiveModeResult = "<<{}>>"
    )

    @Test
    fun invalidEscapeSequenceInLike() = checkInputThrowingEvaluationException(
        """ '' like '^1' escape '^' """,
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        sourceLocationProperties(1, 10) + mapOf(Property.LIKE_ESCAPE to "^", Property.LIKE_PATTERN to "^1")
    )

    @Test
    fun unboundParameters() = checkInputThrowingEvaluationException(
        """SELECT ? FROM <<1>>""",
        ErrorCode.EVALUATOR_UNBOUND_PARAMETER,
        sourceLocationProperties(1, 8) + mapOf(Property.EXPECTED_PARAMETER_ORDINAL to 1, Property.BOUND_PARAMETER_COUNT to 0)
    )

    @Test
    fun searchedCaseNonBooleanPredicate() = checkInputThrowingEvaluationException(
        input = "CASE WHEN 1 THEN 'not gonna happen' ELSE 'permissive mode result' END",
        errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE,
        // TODO:  the call to .booleanValue in the thunk does not have access to metas, so the EvaluationException
        //  is reported to be at the line & column of the CASE statement, not the predicate, unfortunately.
        expectErrorContextValues = sourceLocationProperties(1, 1),
        expectedPermissiveModeResult = "'permissive mode result'"
    )

    @Test
    fun structWithStringAndIntegerKey() = checkInputThrowingEvaluationException(
        input = "{ 'valid_key': 42, 1: 2 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 20L,
            Property.ACTUAL_TYPE to "INT"
        )
    )

    @Test
    fun structWithSymbolAndIntegerKey() = checkInputThrowingEvaluationException(
        input = "{ `valid_key`: 42, 1: 2 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 20L,
            Property.ACTUAL_TYPE to "INT"
        )
    )

    @Test
    fun variableReferenceToIntAsNonTextStructField() =
        checkInputThrowingEvaluationException(
            input = "SELECT {a : 2} FROM {'a' : 1}",
            errorCode = ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
            expectErrorContextValues = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.ACTUAL_TYPE to "INT"
            ),
            expectedPermissiveModeResult = "<<{ '_1': {} }>>"
        )

    fun structWithNullKey() = checkInputThrowingEvaluationException(
        input = "{ 'valid_key': 42, null: 2 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = sourceLocationProperties(1, 1),
        expectedPermissiveModeResult = "{ 'valid_key': 42 }"
    )

    @Test
    fun structWithMissingKey() = checkInputThrowingEvaluationException(
        input = "{ 'valid_key': 42, MISSING: 2 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 20L,
            Property.ACTUAL_TYPE to "MISSING"
        )
    )

    @Test
    fun structWithMultipleInvalidKeys() = checkInputThrowingEvaluationException(
        input = "{ 1: 1, null: 2, missing: 3, true: 4, {}: 5 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 3L,
            Property.ACTUAL_TYPE to "INT"
        )
    )

    @Test
    fun structWithNonTextVariable() = checkInputThrowingEvaluationException(
        input = "{ invalidVar: 1, validVar: 2 }",
        session = mapOf("invalidVar" to "null", "validVar" to "validVarKey").toSession(),
        errorCode = ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 1L,
            Property.ACTUAL_TYPE to "NULL"
        ),
        expectedPermissiveModeResult = "{ 'validVarKey': 2 }"
    )

    @Test
    fun nestedStructWithIntegerKey() = checkInputThrowingEvaluationException(
        input = "{ 'valid_key': 42, 'nestedStruct': { 1: 2, 'valid_key': 42 } }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 38L,
            Property.ACTUAL_TYPE to "INT"
        )
    )

    @Test
    fun structWithIntegerKeyInSFWQuery() = checkInputThrowingEvaluationException(
        input = "SELECT * FROM { 'valid_key': 42, 1: 2 }",
        errorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 34L,
            Property.ACTUAL_TYPE to "INT"
        )
    )

    @Test
    fun trimSpecKeywordBothNotUsedInTrim() = checkInputThrowingEvaluationException(
        input = "SELECT 1 FROM both",
        errorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.BINDING_NAME to "both"
        ),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordLeadingNotUsedInTrim() = checkInputThrowingEvaluationException(
        input = "SELECT 1 FROM leading",
        errorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.BINDING_NAME to "leading"
        ),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordTrailingNotUsedInTrim() = checkInputThrowingEvaluationException(
        input = "SELECT 1 FROM trailing",
        errorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.BINDING_NAME to "trailing"
        ),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordLeadingUsedAsSecondArgInTrim() = checkInputThrowingEvaluationException(
        input = "trim(both leading from 'foo')",
        errorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 11L,
            Property.BINDING_NAME to "leading"
        ),
        expectedPermissiveModeResult = "MISSING"
    )

    // TODO: ORDER BY node is missing metas https://github.com/partiql/partiql-lang-kotlin/issues/516 hence the
    //  incorrect source location in the reported error
    @Test
    fun orderByThrowsCorrectException() =
        // DL TODO: should I consider throwing a semantic exception with that error code?
        assertThrows<NotImplementedError> {
            voidEval("SELECT 1 FROM <<>> ORDER BY x")
        }
}
