package org.partiql.lang.ots_work.interfaces.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.*
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior


abstract class LikeOp: ScalarOp, ArgTypeValidatable {
    override val scalarOpId: ScalarOpId
        get() = ScalarOpId.Like

    final override val defaultReturnTypes: List<CompileTimeType>
        get() = listOf(BoolType.compileTimeType)

    abstract fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult

    abstract fun invoke(value: ExprValue, pattern: ExprValue, escape: ExprValue?): ExprValue
}
