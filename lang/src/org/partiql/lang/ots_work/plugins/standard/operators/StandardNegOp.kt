package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.NegOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.unaryMinus

object StandardNegOp : NegOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        StandardPosOp.inferType(argType)

    override fun invoke(value: ExprValue): ExprValue =
        (-value.numberValue()).exprValue(valueFactory)
}
