package ots.operator

import ots.CompileTimeType
import ots.TypeInferenceResult
import ots.type.BoolType

abstract class LikeOp : ScalarOp, ArgTypeValidatable {
    final override val defaultReturnTypes: List<CompileTimeType>
        get() = listOf(BoolType.compileTimeType)

    abstract fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult
}
