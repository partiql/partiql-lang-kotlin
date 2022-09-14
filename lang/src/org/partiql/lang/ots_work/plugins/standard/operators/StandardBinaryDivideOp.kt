package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.BinaryDivideOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object StandardBinaryDivideOp : BinaryDivideOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)
}
