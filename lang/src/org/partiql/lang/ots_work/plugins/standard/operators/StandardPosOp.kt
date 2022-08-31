package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.PosOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object StandardPosOp : PosOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        when (argType.scalarType) {
            in validOperandTypes -> Successful(argType)
            else -> Failed
        }

    override fun invoke(value: ExprValue): ExprValue {
        // Invoking .numberValue() here makes this essentially just a type check
        value.numberValue()
        // Original value is returned unmodified.
        return value
    }
}
