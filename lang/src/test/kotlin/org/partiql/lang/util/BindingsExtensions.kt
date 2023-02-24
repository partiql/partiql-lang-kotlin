package org.partiql.lang.util

import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.StaticTypeUtils.staticTypeFromExprValueType
import org.partiql.spi.types.StaticType

/**
 * Derives a [Bindings<StaticType>] from a [Bindings<ExprValue>].
 *
 * The primary drawback of this right now is that LIST, SEXP will have an element
 * type of [AnyType] and struct fields will be unknown.
 * TODO:  use schmea-by-example to make this less naive.
 */
fun Bindings<ExprValue>.toTypedBindings() = this.let { valuedBindings ->
    object : Bindings<StaticType> {
        override fun get(bindingName: BindingName): StaticType? {
            val exprValue = valuedBindings[bindingName] ?: return null
            return staticTypeFromExprValueType(exprValue.type)
        }
    }
}
