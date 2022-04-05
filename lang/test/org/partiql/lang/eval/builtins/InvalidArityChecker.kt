package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.propertyValueMapOf

/**
 * This class is used to check arity for ExprFunctions.
 *
 * We check arity with either providing more arguments or less arguments.
 * For example, `char_length()` requires exactly 1 argument.
 *      1) We test with less argument provided: `char_length()`,
 *      2) we test with more argument provided `char_length('a', 'b')`
 *
 * Also, note that the arity check is performed before type check, so we can provide `null` as argument
 * for any ExprFunctions without throwing invalid argument type error, if arity is incorrect.
 */
// TODO: Get rid of `EvaluatorTestBase`
class InvalidArityChecker : EvaluatorTestBase() {

    /**
     * The number of additional arguments beyond the max arity
     */
    private val maxArityOverflow = 5

    /**
     * Generates test cases for arity less than minimum arity and greater than maximum arity for the ExprFunction
     *
     * @param funcName is the name of an ExprFunction.
     * @param maxArity is the maximum arity of an ExprFunction.
     * @param minArity is the minimum arity of an ExprFunction.
     */
    fun checkInvalidArity(funcName: String, minArity: Int, maxArity: Int) {
        if (minArity < 0) throw IllegalStateException("Minimum arity has to be larger than 0.")
        if (maxArity < minArity) throw IllegalStateException("Maximum arity has to be larger than or equal to minimum arity.")

        val sb = StringBuilder("$funcName(")
        val maxInvalidArity = maxArity + maxArityOverflow
        for (curArity in 0..maxInvalidArity) {
            when (curArity) {
                0 -> {} // Don't need to do anything
                1 -> sb.append("null")
                else -> sb.append(",null")
            }
            if (curArity < minArity || curArity > maxArity) { // If less or more argument provided, we catch invalid arity error
                assertThrowsInvalidArity("$sb)", funcName, curArity, minArity, maxArity)
            }
        }
    }

    private fun assertThrowsInvalidArity(
        query: String,
        funcName: String,
        actualArity: Int,
        minArity: Int,
        maxArity: Int
    ) = assertThrows(
        query = query,
        expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
        expectedErrorContext = propertyValueMapOf(
            1, 1,
            Property.FUNCTION_NAME to funcName,
            Property.EXPECTED_ARITY_MIN to minArity,
            Property.EXPECTED_ARITY_MAX to maxArity,
            Property.ACTUAL_ARITY to actualArity
        ),
    )
}
