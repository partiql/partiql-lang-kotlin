package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.*
import org.partiql.lang.ots_work.interfaces.*
import org.partiql.lang.ots_work.interfaces.operators.BinaryDivideOp
import org.partiql.lang.ots_work.plugins.standard.plugin.BehaviorWhenDivisorIsZero
import org.partiql.lang.util.div
import org.partiql.lang.util.isZero
import org.partiql.lang.util.propertyValueMapOf

class StandardBinaryDivideOp(
    val behaviorWhenDivisorIsZero: BehaviorWhenDivisorIsZero?,
    val valueFactory: ExprValueFactory,
    var currentLocationMeta: SourceLocationMeta? = null
) : BinaryDivideOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)

    override fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue {
        behaviorWhenDivisorIsZero ?: error("[behaviorWhenDivisorIsZero] must be defined when invoke scalar DIVIDE operator")

        val denominator = rValue.numberValue()
        if (denominator.isZero()) {
            when (behaviorWhenDivisorIsZero) {
                BehaviorWhenDivisorIsZero.ERROR -> {
                    val errCtx = propertyValueMapOf()
                    currentLocationMeta?.lineNum?.let { errCtx[Property.LINE_NUMBER] = it }
                    currentLocationMeta?.charOffset?.let { errCtx[Property.COLUMN_NUMBER] = it }

                    throw EvaluationException(
                        message = "% by zero",
                        errorCode = ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
                        errorContext = errCtx,
                        cause = null,
                        internal = false
                    )
                }
                BehaviorWhenDivisorIsZero.MISSING -> return valueFactory.missingValue
            }
        }

        try {
            return (lValue.numberValue() / denominator).exprValue(valueFactory)
        } catch (e: ArithmeticException) {
            // Setting the internal flag as true as it is not clear what
            // ArithmeticException may be thrown by the above
            throw EvaluationException(
                cause = e,
                errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                internal = true
            )
        }
    }
}