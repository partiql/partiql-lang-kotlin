package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.err
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.ScalarType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operators.BinaryModuloOp
import org.partiql.lang.util.isZero
import org.partiql.lang.util.rem

class StandardBinaryModuloOp(
    val valueFactory: ExprValueFactory
) : BinaryModuloOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)

    override fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue {
        val denominator = rValue.numberValue()
        if (denominator.isZero()) {
            err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, null, false)
        }

        return (lValue.numberValue() % denominator).exprValue(valueFactory)
    }
}
