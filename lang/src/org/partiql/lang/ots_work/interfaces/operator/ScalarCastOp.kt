package org.partiql.lang.ots_work.interfaces.operator

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult

abstract class ScalarCastOp : ScalarOp {
    override val scalarOpId: ScalarOpId
        get() = ScalarOpId.ScalarCast

    /**
     * CAST is going to return MISSING if type inference fails, which is not customizable
     */
    final override val defaultReturnTypes: List<CompileTimeType>
        get() = emptyList()

    abstract fun inferType(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult

    // TODO: Make return type not nullable once we handle error in OTS properly
    abstract fun invoke(sourceValue: ExprValue, targetType: CompileTimeType): ExprValue?
}
