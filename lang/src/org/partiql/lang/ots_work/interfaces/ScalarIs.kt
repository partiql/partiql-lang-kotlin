package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior

/**
 * I prefer IS operator to be not customizable, and just use `ScalarType.validateValue(value: ExprValue)` to invoke it.
 * This interface only works as a workaround for different IS evaluation behaviors with different [TypedOpBehavior].
 * Once we decide to remove [TypedOpBehavior], we can remove this interface as well.
 */
interface ScalarIs {
    fun invoke(value: ExprValue, targetType: CompileTimeType): Boolean
}
