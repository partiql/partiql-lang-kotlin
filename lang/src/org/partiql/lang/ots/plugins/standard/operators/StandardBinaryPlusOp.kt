package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.operator.BinaryPlusOp
import org.partiql.lang.ots.interfaces.type.ScalarType

object StandardBinaryPlusOp : BinaryPlusOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)
}
