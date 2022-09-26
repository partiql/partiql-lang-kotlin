package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.operator.NegOp
import org.partiql.lang.ots.interfaces.type.ScalarType

object StandardNegOp : NegOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        StandardPosOp.inferType(argType)
}
