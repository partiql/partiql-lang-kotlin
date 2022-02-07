package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.ErrorCode.EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE
import org.partiql.lang.errors.Property.ACTUAL_ARGUMENT_TYPES
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.ExprValueType.INT
import org.partiql.lang.eval.ExprValueType.STRING
import org.partiql.lang.eval.ExprValueType.STRUCT
import org.partiql.lang.eval.ExprValueType.SYMBOL
import org.partiql.lang.eval.ExprValueType.TIMESTAMP
import org.partiql.lang.util.sourceLocationProperties

class ConcatEvaluationTest : EvaluatorTestBase() {
    private val argumentTypeMap = mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRING, SYMBOL).toString())

    @Test
    fun concatWrongLeftType() = 
        checkInputThrowingEvaluationException("1 || 'a'",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 3) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(INT, STRING).toString()),
                                              expectedPermissiveModeResult = "MISSING")

    @Test
    fun concatWrongRightType() =
        checkInputThrowingEvaluationException("'a' || 1",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 5) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRING, INT).toString()),
                                              expectedPermissiveModeResult = "MISSING")

    @Test
    fun concatWrongBothTypes() =
        checkInputThrowingEvaluationException("{} || `2010T`",
                                              EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                                              sourceLocationProperties(1, 4) +
                                              mapOf(ACTUAL_ARGUMENT_TYPES to listOf(STRUCT, TIMESTAMP).toString()),
                                              expectedPermissiveModeResult = "MISSING")

    @Test
    fun strings() = assertEval("'a' || 'b'", "\"ab\"")

    @Test
    fun symbols() = assertEval("`'a'` || `'b'`", "\"ab\"")

    @Test
    fun stringAndSymbols() = assertEval("'a' || `'b'`", "\"ab\"")

    @Test
    fun nullAndString() = assertEval("null || 'b'", "null")

    @Test
    fun missingAndString() = assertEval("missing || 'b'", "null")
}
