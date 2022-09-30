package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.BoolType
import OTS.ITF.org.partiql.ots.type.ScalarType

object LegacyNotOp : ScalarOp {
    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(BoolType.compileTimeType)

    override val validOperandTypes: List<ScalarType> =
        listOf(BoolType)

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult {
        require(argsType.size == 1) { "NOT Operator expects 1 argument" }

        val argType = argsType[0]

        return when (argType.scalarType) {
            BoolType -> Successful(argType)
            else -> Failed
        }
    }
}
