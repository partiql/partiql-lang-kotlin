package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.NegOp
import OTS.ITF.org.partiql.ots.type.ScalarType

object StandardNegOp : NegOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        StandardPosOp.inferType(argType)
}
