package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.ScalarType

object LegacyNegOp : ScalarOp {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult {
        require(argsType.size == 1) { "NEG Operator expects 1 argument" }

        val argType = argsType[0]

        return when (argType.scalarType) {
            in LegacyPosOp.validOperandTypes -> Successful(argType)
            else -> Failed
        }
    }
}
