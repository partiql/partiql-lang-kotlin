package org.partiql.lang.eval.internal

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.Named
import org.partiql.lang.eval.stringify
import org.partiql.lang.util.downcast

/**
 * An [ExprValue] that also implements [Named].
 */
internal class NamedExprValue(override val name: ExprValue, val value: ExprValue) : ExprValue by value, Named {
    override fun <T : Any?> asFacet(type: Class<T>?): T? = downcast(type) ?: value.asFacet(type)

    override fun toString(): String = stringify()
}
