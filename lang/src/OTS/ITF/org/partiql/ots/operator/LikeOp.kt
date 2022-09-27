package OTS.ITF.org.partiql.ots.operator

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.type.BoolType

abstract class LikeOp : ScalarOp, ArgTypeValidatable {
    final override val defaultReturnTypes: List<CompileTimeType>
        get() = listOf(BoolType.compileTimeType)

    abstract fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult
}
