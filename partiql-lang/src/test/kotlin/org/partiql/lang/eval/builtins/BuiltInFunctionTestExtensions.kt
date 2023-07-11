package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.newFromIonText
import org.partiql.value.datetime.DateTimeValue

/**
 * Internal function used by ExprFunctionTest to test invalid argument type.
 */
internal val invalidArgTypeChecker = InvalidArgTypeChecker()
internal fun checkInvalidArgType(funcName: String, syntaxSuffix: String = "(", args: List<Argument>) =
    invalidArgTypeChecker.checkInvalidArgType(funcName, syntaxSuffix, args)

/**
 * Internal function used by ExprFunctionTest to test invalid arity.
 */
internal val invalidArityChecker = InvalidArityChecker()
internal fun checkInvalidArity(
    funcName: String,
    minArity: Int,
    maxArity: Int,
    targetPipeline: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES
) =
    invalidArityChecker.checkInvalidArity(funcName, minArity, maxArity, targetPipeline)

private fun String.toExprValue(): ExprValue = newFromIonText(this)

private fun Map<String, String>.toBindings(): Bindings<ExprValue> = Bindings.ofMap(mapValues { it.value.toExprValue() })

/**
 * Internal function used by ExprFunctionTest to build EvaluationSession.
 */
internal fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

/**
 * Internal function used by ExprFunctionTest to build EvaluationSession with now.
 */
internal fun buildSessionWithNow(numMillis: Long, localOffset: Int) =
    EvaluationSession.build { nowZ(DateTimeValue.timestamp(numMillis.toBigDecimal().movePointLeft(3), org.partiql.value.datetime.TimeZone.UtcOffset.of(localOffset))) }

/**
 * Used by ExprFunctionTest to represent a test case.
 */
data class ExprFunctionTestCase(
    val source: String,
    val expectedLegacyModeResult: String,
    val expectedPermissiveModeResult: String = expectedLegacyModeResult,
    val session: EvaluationSession = EvaluationSession.standard()
)
