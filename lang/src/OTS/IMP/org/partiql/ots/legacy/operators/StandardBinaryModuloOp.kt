package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.BinaryModuloOp
import OTS.ITF.org.partiql.ots.type.ScalarType

object StandardBinaryModuloOp : BinaryModuloOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult =
        inferTypeOfArithmeticOp(argsType)
}
