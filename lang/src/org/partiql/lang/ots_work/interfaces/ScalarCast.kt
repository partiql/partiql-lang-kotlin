package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.eval.ExprValue

interface ScalarCast {
    fun inferType(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult

    /**
     * Null return value means there is such cast is not allowed.
     */
    fun invoke(value: ExprValue, targetType: CompileTimeType): ExprValue?
}
