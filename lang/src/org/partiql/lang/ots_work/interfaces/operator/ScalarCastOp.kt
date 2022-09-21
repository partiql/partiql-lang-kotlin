package org.partiql.lang.ots_work.interfaces.operator

import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult

abstract class ScalarCastOp : ScalarOp {
    /**
     * CAST is going to return MISSING if type inference fails, which is not customizable
     */
    final override val defaultReturnTypes: List<CompileTimeType>
        get() = emptyList()

    abstract fun inferType(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult
}
