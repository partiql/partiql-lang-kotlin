package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.PosOp
import OTS.ITF.org.partiql.ots.type.ScalarType

object StandardPosOp : PosOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult {
        require(argsType.size == 1) { "POS Operator expects 1 argument" }

        val argType = argsType[0]

        return when (argType.scalarType) {
            in validOperandTypes -> Successful(argType)
            else -> Failed
        }
    }
}
