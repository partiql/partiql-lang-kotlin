package org.partiql.lang.ots_work.interfaces.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.BoolType
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.ScalarOp
import org.partiql.lang.ots_work.interfaces.ScalarOpId
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior

/**
 * I prefer IS operator to be not customizable, and just use `ScalarType.validateValue(value: ExprValue)` to invoke it.
 * This interface only works as a workaround for different IS evaluation behaviors with different [TypedOpBehavior].
 * Once we decide to remove [TypedOpBehavior], we can remove this interface as well.
 */
abstract class ScalarIsOp : ScalarOp {
    override val scalarOpId: ScalarOpId
        get() = ScalarOpId.ScalarIs

    /**
     * Return type of IS operator is BOOLEAN
     */
    final override val defaultReturnTypes: List<CompileTimeType>
        get() = listOf(BoolType.compileTimeType)

    abstract fun invoke(value: ExprValue, targetType: CompileTimeType): Boolean
}
