package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.to

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
    fun lessArguments() {
        checkInputThrowingEvaluationException(
            "size()",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            // Kotlin auto-casts the values of EXPECTED_ARITY_MIN and EXPECTED_ARITY_MAX to Long
            // unless we explicitly specify [Any] as for the value generic argument of the map below
            mapOf<Property, Any>(
                Property.FUNCTION_NAME to "size",
                Property.EXPECTED_ARITY_MIN to 1,
                Property.EXPECTED_ARITY_MAX to 1,
                Property.ACTUAL_ARITY to 0,
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L))
    }
    
    @Test
    fun moreArguments() =
        checkInputThrowingEvaluationException("size(null, null)",
            ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            // Kotlin auto-casts the values of EXPECTED_ARITY_MIN and EXPECTED_ARITY_MAX to Long
            // unless we explicitly specify [Any] as for the value generic argument of the map below
            mapOf<Property, Any>(
                Property.FUNCTION_NAME to "size",
                Property.EXPECTED_ARITY_MIN to 1,
                Property.EXPECTED_ARITY_MAX to 1,
                Property.ACTUAL_ARITY to 2,
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L))


    @Test
    fun wrongTypeOfArgument() =
        checkInputThrowingEvaluationException("size(1)",
          ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
          mapOf(Property.EXPECTED_ARGUMENT_TYPES to "LIST, BAG, STRUCT, or SEXP",
                Property.ARGUMENT_POSITION to 1,
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.FUNCTION_NAME to "size",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
          expectedPermissiveModeResult = "MISSING")
}