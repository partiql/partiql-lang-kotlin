package OTS.IMP.org.partiql.ots.legacy.operators

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Failed
import OTS.ITF.org.partiql.ots.Successful
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.Uncertain
import OTS.ITF.org.partiql.ots.operator.LikeOp
import OTS.ITF.org.partiql.ots.type.BoolType
import OTS.ITF.org.partiql.ots.type.ScalarType

object StandardLikeOp : LikeOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult {
        require(argsType.size == 2 || argsType.size == 3) { "LIKE operator expects 2-3 arguments" }

        val value = argsType[0]
        val pattern = argsType[1]
        val escape = argsType.getOrNull(2)

        return when {
            value.scalarType !in validOperandTypes || pattern.scalarType !in validOperandTypes -> Failed
            escape === null -> Successful(BoolType.compileTimeType)
            escape.scalarType in validOperandTypes -> Uncertain(BoolType.compileTimeType)
            else -> Failed
        }
    }
}
