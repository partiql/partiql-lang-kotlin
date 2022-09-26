package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Failed
import org.partiql.lang.ots.interfaces.Successful
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.operator.PosOp
import org.partiql.lang.ots.interfaces.type.ScalarType

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
}
