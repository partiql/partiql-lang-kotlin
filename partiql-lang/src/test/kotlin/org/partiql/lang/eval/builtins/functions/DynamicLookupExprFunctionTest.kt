package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.DYNAMIC_LOOKUP_FUNCTION_NAME
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class DynamicLookupExprFunctionTest : EvaluatorTestBase() {
    val session = mapOf(
        "f" to "{ foo: 42 }",
        "b" to "{ bar: 43 }",
        "foo" to "44",
    ).toSession()

    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ToStringPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = testCase.source,
            session = session,
            expectedResult = testCase.expectedLegacyModeResult,
            expectedResultFormat = ExpectedResultFormat.ION,
            target = EvaluatorTestTarget.PARTIQL_PIPELINE
        )

    // We rely on the built-in [DEFAULT_COMPARATOR] for the actual definition of equality, which is not being tested
    // here.
    class ToStringPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // function signature: $__dynamic_lookup__(<symbol>, <symbol>, <symbol>, <any>*)
            // arg #1: the name of the field or variable to locate.
            // arg #2: case-insensitive or sensitive
            // arg #3: look in globals first or locals first.
            // arg #4 and later (variadic): any remaining arguments are the variables to search within, which in general
            // are structs.  note that in general, these will be local variables, however we don't use local variables
            // here to simplify these test cases.

            // locals_then_globals

            // `foo` should be found in the variable f, which is a struct
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, `case_insensitive`, `locals_then_globals`, f, b)", "42"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`fOo`, `case_insensitive`, `locals_then_globals`, f, b)", "42"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`FoO`, `case_insensitive`, `locals_then_globals`, f, b)", "42"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, `case_sensitive`, `locals_then_globals`, f, b)", "42"),
            // `bar` should be found in the variable b, which is also a struct
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bar`, `case_insensitive`, `locals_then_globals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`BaR`, `case_insensitive`, `locals_then_globals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bAr`, `case_insensitive`, `locals_then_globals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bar`, `case_sensitive`, `locals_then_globals`, f, b)", "43"),

            // globals_then_locals

            // The global variable `foo` should be found first, ignoring the `f.foo`, unlike the similar cases above`
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, `case_insensitive`, `globals_then_locals`, f, b)", "44"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`fOo`, `case_insensitive`, `globals_then_locals`, f, b)", "44"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`FoO`, `case_insensitive`, `globals_then_locals`, f, b)", "44"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, `case_sensitive`, `globals_then_locals`, f, b)", "44"),
            // `bar` should still be found in the variable b, which is also a struct, since there is no global named `bar`.
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bar`, `case_insensitive`, `globals_then_locals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`BaR`, `case_insensitive`, `globals_then_locals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bAr`, `case_insensitive`, `globals_then_locals`, f, b)", "43"),
            ExprFunctionTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bar`, `case_sensitive`, `globals_then_locals`, f, b)", "43")
        )
    }

    @ParameterizedTest
    @ArgumentsSource(MismatchCaseSensitiveCases::class)
    fun mismatchedCaseSensitiveTests(testCase: EvaluatorErrorTestCase) =
        runEvaluatorErrorTestCase(
            testCase.copy(
                expectedPermissiveModeResult = "MISSING",
                targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE
            ),
            session = session
        )

    class MismatchCaseSensitiveCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Can't find these variables due to case mismatch when perform case sensitive lookup
            EvaluatorErrorTestCase(
                query = "\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`fOo`, `case_sensitive`, `locals_then_globals`, f, b)",
                expectedErrorCode = ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(1, 1, Property.BINDING_NAME to "fOo")
            ),
            EvaluatorErrorTestCase(
                query = "\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`FoO`, `case_sensitive`, `locals_then_globals`, f, b)",
                expectedErrorCode = ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(1, 1, Property.BINDING_NAME to "FoO")
            ),
            EvaluatorErrorTestCase(
                query = "\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`BaR`, `case_sensitive`, `locals_then_globals`, f, b)",
                expectedErrorCode = ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(1, 1, Property.BINDING_NAME to "BaR")
            ),
            EvaluatorErrorTestCase(
                query = "\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`bAr`, `case_sensitive`, `locals_then_globals`, f, b)",
                expectedErrorCode = ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(1, 1, Property.BINDING_NAME to "bAr")
            )
        )
    }

    data class InvalidArgTestCase(
        val source: String,
        val argumentPosition: Int,
        val actualArgumentType: String,
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun invalidArgTypeTestCases(testCase: InvalidArgTestCase) =
        runEvaluatorErrorTestCase(
            query = testCase.source,
            expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectedErrorContext = propertyValueMapOf(
                1, 1,
                Property.FUNCTION_NAME to DYNAMIC_LOOKUP_FUNCTION_NAME,
                Property.EXPECTED_ARGUMENT_TYPES to "SYMBOL",
                Property.ACTUAL_ARGUMENT_TYPES to testCase.actualArgumentType,
                Property.ARGUMENT_POSITION to testCase.argumentPosition
            ),
            expectedPermissiveModeResult = "MISSING",
            target = EvaluatorTestTarget.PARTIQL_PIPELINE
        )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            InvalidArgTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(1, `case_insensitive`, `locals_then_globals`)", 1, "INT"),
            InvalidArgTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, 1, `locals_then_globals`)", 2, "INT"),
            InvalidArgTestCase("\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"(`foo`, `case_insensitive`, 1)", 3, "INT")
        )
    }

    @Test
    fun invalidArityTest() = checkInvalidArity(
        funcName = "\"$DYNAMIC_LOOKUP_FUNCTION_NAME\"",
        maxArity = Int.MAX_VALUE,
        minArity = 3,
        targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE
    )
}
