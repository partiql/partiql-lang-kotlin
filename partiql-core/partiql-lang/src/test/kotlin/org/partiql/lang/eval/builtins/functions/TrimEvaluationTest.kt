package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class TrimEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(CharLengthPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class CharLengthPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("trim('   string   ')", "\"string\""),
            ExprFunctionTestCase("trim('   string')", "\"string\""),
            ExprFunctionTestCase("trim('string   ')", "\"string\""),
            ExprFunctionTestCase("trim('\tstring\t')", "\"\tstring\t\""),
            ExprFunctionTestCase("trim(from '   string   ')", "\"string\""),
            ExprFunctionTestCase("trim(from '   string')", "\"string\""),
            ExprFunctionTestCase("trim(from 'string   ')", "\"string\""),
            ExprFunctionTestCase("trim(both from '   string   ')", "\"string\""),
            ExprFunctionTestCase("trim(both from '   string')", "\"string\""),
            ExprFunctionTestCase("trim(both from 'string   ')", "\"string\""),
            ExprFunctionTestCase("trim(leading from '   string   ')", "\"string   \""),
            ExprFunctionTestCase("trim(leading from '   string')", "\"string\""),
            ExprFunctionTestCase("trim(leading from 'string   ')", "\"string   \""),
            ExprFunctionTestCase("trim(trailing from '   string   ')", "\"   string\""),
            ExprFunctionTestCase("trim(trailing from '   string')", "\"   string\""),
            ExprFunctionTestCase("trim(trailing from 'string   ')", "\"string\""),
            ExprFunctionTestCase("trim(both ' -=' from '- =string =-  ')", "\"string\""),
            ExprFunctionTestCase("trim(both ' -=' from '--===    -= -= -=   string')", "\"string\""),
            ExprFunctionTestCase("trim(both ' -=' from 'string ==- = -=- - ----------  ')", "\"string\""),
            ExprFunctionTestCase("trim(both ' ' from '            ')", "\"\""),
            ExprFunctionTestCase("trim(leading ' ' from '            ')", "\"\""),
            ExprFunctionTestCase("trim(trailing ' ' from '            ')", "\"\""),
            ExprFunctionTestCase(
                "trim(both '💩' from  '💩💩💩💩💩💩💩💩💩💩😁😞😸😸💩💩💩💩💩💩💩💩💩💩💩💩💩💩')",
                "\"😁😞😸😸\""
            ),
            ExprFunctionTestCase("trim('               😁😞😸😸             ')", "\"😁😞😸😸\""),
            ExprFunctionTestCase("trim(both '話 ' from '話話 話話話話話話費谷料村能話話話話 話話話話    ')", "\"費谷料村能\""),
            ExprFunctionTestCase("trim('   a'||'b  ')", "\"ab\""),
            ExprFunctionTestCase(
                """ 
                    SELECT trim(both el from '   1ab1  ') AS trimmed 
                    FROM <<' 1'>> AS el 
                """.trimIndent(),
                "$BAG_ANNOTATION::[{trimmed:\"ab\"}]"
            ),
            ExprFunctionTestCase("trim('12' from '1212b1212')", "\"b\""),
            ExprFunctionTestCase("trim('12' from '1212b')", "\"b\""),
            ExprFunctionTestCase("trim('12' from 'b1212')", "\"b\""),
            ExprFunctionTestCase("trim(both null from '')", "null"),
            ExprFunctionTestCase("trim(both '' from null)", "null"),
            ExprFunctionTestCase("trim(null from '')", "null"),
            ExprFunctionTestCase("trim('' from null)", "null"),
            ExprFunctionTestCase("trim(null)", "null"),
            ExprFunctionTestCase("trim(both missing from '')", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("trim(both '' from missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("trim(missing from '')", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("trim('' from missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("trim(missing)", "null", "$MISSING_ANNOTATION::null"),
            // test for upper case trim spec
            ExprFunctionTestCase("trim(BOTH from '   string   ')", "\"string\""),
            ExprFunctionTestCase("trim(LEADING from '   string   ')", "\"string   \""),
            ExprFunctionTestCase("trim(TRAILING from 'string   ')", "\"string\""),
        )
    }

    // Consider trim(something from ' string '), here "something" will be interpreted as an identifier
    @Test
    fun trimSubstringNoBinding() = runEvaluatorErrorTestCase(
        query = "trim(something from ' string ')",
        expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
        expectedErrorContext = propertyValueMapOf(1, 6, Property.BINDING_NAME to "something"),
        expectedPermissiveModeResult = "MISSING"
    )

    // Consider trim(something ' ' from ' string '), here "something" will be interpreted as a trim specification
    // this test case is tested currently in ParserErrorsTests
    // todo: decide where should we catch the error and clean up the comments

    // Error test cases: Invalid argument type
    @Test
    fun trimInvalidArgTypeTest1() = checkInvalidArgType(
        funcName = "trim",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    @Test
    fun trimInvalidArgTypeTest2() = checkInvalidArgType(
        funcName = "trim",
        syntaxSuffix = "(trailing ",
        args = listOf(
            Argument(2, StaticType.STRING, " from "),
            Argument(3, StaticType.STRING, ")")
        )
    )

    @Test
    fun trimInvalidArgTypeTest3() = checkInvalidArgType(
        funcName = "trim",
        syntaxSuffix = "(from ",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
