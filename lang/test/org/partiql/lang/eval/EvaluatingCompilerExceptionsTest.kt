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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.rootCause
import org.partiql.lang.util.to

class EvaluatingCompilerExceptionsTest : EvaluatorTestBase() {

    // new tests in this file should be added here or parameterized like this with another @ParameterizedTest.
    // No time to refactor this entire test class to be parameterized but neither should we continue
    // to follow a pattern that we'd like to change anyway.
    // FIXME - these tests don't seem to work, and when enabled the options are set but the `FLOAT` type is missing
    //         the parameter at the point we test it in the EvaluatingCompiler
    // XXX - for some reason, @Ignore did not work on this parameterized test.
    @Disabled
    @ParameterizedTest
    @ArgumentsSource(ErrorTestCasesTestCases::class)
    fun errorTestCases(tc: EvaluatorErrorTestCase) =
        assertThrows(tc, EvaluationSession.standard())
    class ErrorTestCasesTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            EvaluatorErrorTestCase(
                """CAST(12 AS FLOAT(1))""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                propertyValueMapOf(1, 13),
                compileOptionsBuilderBlock = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS.optionsBlock
            ),
            EvaluatorErrorTestCase(
                """CAN_CAST(12 AS FLOAT(1))""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                propertyValueMapOf(1, 17),
                compileOptionsBuilderBlock = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS.optionsBlock
            ),
            EvaluatorErrorTestCase(
                """12 IS FLOAT(1)""",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                propertyValueMapOf(1, 8),
                compileOptionsBuilderBlock = CompOptions.TYPED_OP_BEHAVIOR_HONOR_PARAMS.optionsBlock
            )
        )
    }

    @Test
    fun notOnOne() = assertThrows(
        "not 1",
        ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE,
        expectedErrorContext = propertyValueMapOf(1, 1),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun betweenIncompatiblePredicate() = assertThrows(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x BETWEEN 'A' AND 2
        """,
        ErrorCode.EVALUATOR_INVALID_COMPARISION,
        expectedErrorContext = propertyValueMapOf(4, 19),
        expectedPermissiveModeResult = "<<>>"
    )

    @Test
    fun notBetweenIncompatiblePredicate() = assertThrows(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x NOT BETWEEN 1 AND 'Y'
        """,
        ErrorCode.EVALUATOR_INVALID_COMPARISION,
        expectedErrorContext = propertyValueMapOf(4, 19),
        expectedPermissiveModeResult = "<<>>"
    )

    @Test
    fun shadowedVariables() = assertThrows(
        """SELECT VALUE a FROM `[{v:5}]` AS item, @item.v AS a, @item.v AS a""",
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        expectedErrorContext = propertyValueMapOf(1, 14, Property.BINDING_NAME to "a", Property.BINDING_NAME_MATCHES to "a, a"),
        expectedPermissiveModeResult = "<<MISSING>>"
    )

    @Test
    fun topLevelCountStar() = assertThrows(
        """COUNT(*)""",
        ErrorCode.EVALUATOR_COUNT_START_NOT_ALLOWED,
        expectedErrorContext = propertyValueMapOf(1, 1)
    )

    @Test
    fun selectValueCountStar() = assertThrows(
        """SELECT VALUE COUNT(*) FROM numbers""",
        ErrorCode.EVALUATOR_COUNT_START_NOT_ALLOWED,
        expectedErrorContext = propertyValueMapOf(1, 14)
    )

    @Test
    fun selectListNestedAggregateCall() = assertThrows(
        """SELECT SUM(AVG(n)) FROM <<numbers, numbers>> AS n""",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        expectedErrorContext = propertyValueMapOf(1, 12)
    )

    private val sqlWithUndefinedVariable = "SELECT VALUE y FROM << 'el1' >> AS x"
    @Test
    fun badAlias() {
        // Note that the current default for CompileOptions.undefinedVariable is UndefinedVariableBehavior.ERROR
        assertThrows(
            sqlWithUndefinedVariable,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            propertyValueMapOf(1, 14, Property.BINDING_NAME to "y"),
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
        assertThrows(
            sqlWithUndefinedQuotedVariable,
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            propertyValueMapOf(1, 14, Property.BINDING_NAME to "y"),
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
    fun wrongArityExists() = assertThrows(
        "exists()",
        ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
        expectedErrorContext = propertyValueMapOf(
            1, 1,
            Property.EXPECTED_ARITY_MIN to 1,
            Property.EXPECTED_ARITY_MAX to 1,
            Property.ACTUAL_ARITY to 0,
            Property.FUNCTION_NAME to "exists"
        )
    )

    @Test
    fun unknownFunction() = assertThrows(
        "unknownFunction()",
        ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.FUNCTION_NAME to "unknownfunction")
    )

    @Test
    fun rightJoin() = assertThrows(
        "SELECT * FROM animals AS a RIGHT CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id",
        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
        expectedErrorContext = propertyValueMapOf(1, 28, Property.FEATURE_NAME to "RIGHT and FULL JOIN")
    )

    @Test
    fun outerJoin() = assertThrows(
        "SELECT * FROM animals AS a OUTER CROSS JOIN animal_types AS a_type WHERE a.type = a_type.id",
        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
        expectedErrorContext = propertyValueMapOf(1, 28, Property.FEATURE_NAME to "RIGHT and FULL JOIN")
    )

    @Test
    fun addingWrongTypes() = assertThrows(
        "1 + 2 + 4 + 'a' + 5",
        ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE,
        expectedErrorContext = propertyValueMapOf(1, 11),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun badCastToInt() = assertThrows(
        "CAST('a' as int) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        propertyValueMapOf(1, 1, Property.CAST_FROM to "STRING", Property.CAST_TO to "INT"),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun badCastInSelectToInt() = assertThrows(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE CAST(_2 as INT) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        propertyValueMapOf(1, 75, Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INT"),
        expectedPermissiveModeResult = "<<{'_1': `a`, '_2': 1}, {'_1': `a`, '_2': 3}>>"
    )

    @Test
    fun badCastToDecimal() = assertThrows(
        "CAST('a' as DECIMAL) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        propertyValueMapOf(1, 1, Property.CAST_FROM to "STRING", Property.CAST_TO to "DECIMAL"),
        expectedPermissiveModeResult = "MISSING"
    ) {
        assertEquals(NumberFormatException::class, it.rootCause?.javaClass)
    }

    @Test
    fun badCastToTimestamp() = assertThrows(
        "CAST('2010-01-01T10' as TIMESTAMP) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        propertyValueMapOf(1, 1, Property.CAST_FROM to "STRING", Property.CAST_TO to "TIMESTAMP"),
        expectedPermissiveModeResult = "MISSING"
    ) {
        assertEquals(IllegalArgumentException::class, it.rootCause?.javaClass)
    }

    @Test
    fun divideByZero() = assertThrows(
        "1 / 0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        propertyValueMapOf(1, 3),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun divideByZeroDecimal() = assertThrows(
        "1.0 / 0.0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        propertyValueMapOf(1, 5),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun moduloByZero() = assertThrows(
        "1 % 0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        propertyValueMapOf(1, 3),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun moduloByZeroDecimal() = assertThrows(
        "1.0 % 0.0",
        ErrorCode.EVALUATOR_MODULO_BY_ZERO,
        propertyValueMapOf(1, 5),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun divideByZeroInSelect() = assertThrows(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 2}, {_1: a, _2: 3}]` WHERE _2 / 0 > 0",
        ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
        expectedErrorContext = propertyValueMapOf(1, 76),
        expectedPermissiveModeResult = "<<>>"
    )

    @Test
    fun utcnowWithArgument() = assertThrows(
        "utcnow(1)",
        ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.EXPECTED_ARITY_MIN to 0, Property.EXPECTED_ARITY_MAX to 0, Property.ACTUAL_ARITY to 1, Property.FUNCTION_NAME to "utcnow")
    )

    @Test
    fun ambiguousFieldOnStructCaseSensitiveLookup() = assertThrows(
        """ select "repeated" from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        propertyValueMapOf(1, 9, Property.BINDING_NAME to "repeated", Property.BINDING_NAME_MATCHES to "repeated, repeated"),
        expectedPermissiveModeResult = "<<{}>>"
    )

    @Test
    fun ambiguousFieldOnStructCaseInsensitiveLookup() = assertThrows(
        """ select REPEATED from `[{repeated:1, repeated:2}]` """,
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        propertyValueMapOf(1, 9, Property.BINDING_NAME to "REPEATED", Property.BINDING_NAME_MATCHES to "repeated, repeated"),
        expectedPermissiveModeResult = "<<{}>>"
    )

    @Test
    fun invalidEscapeSequenceInLike() = assertThrows(
        """ '' like '^1' escape '^' """,
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        propertyValueMapOf(1, 10, Property.LIKE_ESCAPE to "^", Property.LIKE_PATTERN to "^1")
    )

    @Test
    fun unboundParameters() = assertThrows(
        """SELECT ? FROM <<1>>""",
        ErrorCode.EVALUATOR_UNBOUND_PARAMETER,
        propertyValueMapOf(1, 8, Property.EXPECTED_PARAMETER_ORDINAL to 1, Property.BOUND_PARAMETER_COUNT to 0)
    )

    @Test
    fun searchedCaseNonBooleanPredicate() = assertThrows(
        query = "CASE WHEN 1 THEN 'not gonna happen' ELSE 'permissive mode result' END",
        expectedErrorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE,
        // TODO:  the call to .booleanValue in the thunk does not have access to metas, so the EvaluationException
        //  is reported to be at the line & column of the CASE statement, not the predicate, unfortunately.
        expectedErrorContext = propertyValueMapOf(1, 1),
        expectedPermissiveModeResult = "'permissive mode result'"
    )

    @Test
    fun structWithStringAndIntegerKey() = assertThrows(
        query = "{ 'valid_key': 42, 1: 2 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 20, Property.ACTUAL_TYPE to "INT")
    )

    @Test
    fun structWithSymbolAndIntegerKey() = assertThrows(
        query = "{ `valid_key`: 42, 1: 2 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 20, Property.ACTUAL_TYPE to "INT")
    )

    @Test
    fun variableReferenceToIntAsNonTextStructField() =
        assertThrows(
            query = "SELECT {a : 2} FROM {'a' : 1}",
            expectedErrorCode = ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
            expectedErrorContext = propertyValueMapOf(1, 8, Property.ACTUAL_TYPE to "INT"),
            expectedPermissiveModeResult = "<<{ '_1': {} }>>"
        )

    fun structWithNullKey() = assertThrows(
        query = "{ 'valid_key': 42, null: 2 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 1),
        expectedPermissiveModeResult = "{ 'valid_key': 42 }"
    )

    @Test
    fun structWithMissingKey() = assertThrows(
        query = "{ 'valid_key': 42, MISSING: 2 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 20, Property.ACTUAL_TYPE to "MISSING")
    )

    @Test
    fun structWithMultipleInvalidKeys() = assertThrows(
        query = "{ 1: 1, null: 2, missing: 3, true: 4, {}: 5 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 3, Property.ACTUAL_TYPE to "INT")
    )

    @Test
    fun structWithNonTextVariable() = assertThrows(
        query = "{ invalidVar: 1, validVar: 2 }",
        session = mapOf("invalidVar" to "null", "validVar" to "validVarKey").toSession(),
        expectedErrorCode = ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.ACTUAL_TYPE to "NULL"),
        expectedPermissiveModeResult = "{ 'validVarKey': 2 }"
    )

    @Test
    fun nestedStructWithIntegerKey() = assertThrows(
        query = "{ 'valid_key': 42, 'nestedStruct': { 1: 2, 'valid_key': 42 } }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 38, Property.ACTUAL_TYPE to "INT")
    )

    @Test
    fun structWithIntegerKeyInSFWQuery() = assertThrows(
        query = "SELECT * FROM { 'valid_key': 42, 1: 2 }",
        expectedErrorCode = ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
        expectedErrorContext = propertyValueMapOf(1, 34, Property.ACTUAL_TYPE to "INT")
    )

    @Test
    fun trimSpecKeywordBothNotUsedInTrim() = assertThrows(
        query = "SELECT 1 FROM both",
        expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectedErrorContext = propertyValueMapOf(1, 15, Property.BINDING_NAME to "both"),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordLeadingNotUsedInTrim() = assertThrows(
        query = "SELECT 1 FROM leading",
        expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectedErrorContext = propertyValueMapOf(1, 15, Property.BINDING_NAME to "leading"),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordTrailingNotUsedInTrim() = assertThrows(
        query = "SELECT 1 FROM trailing",
        expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectedErrorContext = propertyValueMapOf(1, 15, Property.BINDING_NAME to "trailing"),
        expectedPermissiveModeResult = "<<{'_1':1}>>"
    )

    @Test
    fun trimSpecKeywordLeadingUsedAsSecondArgInTrim() = assertThrows(
        query = "trim(both leading from 'foo')",
        expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectedErrorContext = propertyValueMapOf(1, 11, Property.BINDING_NAME to "leading"),
        expectedPermissiveModeResult = "MISSING"
    )
}
