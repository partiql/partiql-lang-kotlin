package org.partiql.ast.helpers

import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.ast
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

private val col = { index: Int -> "_${index + 1}" }

/**
 * Produces a "binder" (AS alias) for an expression following the given rules:
 *
 *  1. If item is an id, use the last symbol
 *  2. If item is a path with a final symbol step, use the symbol — else 4
 *  3. If item is a cast, use the value name
 *  4. If item is a Session Attribute, use the variable name (similar to Expr.Var)
 *  5. Else, use item index with prefix _
 *
 *  See https://github.com/partiql/partiql-lang-kotlin/issues/1122
 */
public fun Expr.toBinder(index: Int): Identifier.Symbol = when (this) {
    is Expr.Var -> this.identifier.toBinder()
    is Expr.Path -> this.toBinder(index)
    is Expr.Cast -> this.value.toBinder(index)
    is Expr.SessionAttribute -> this.attribute.name.toBinder()
    else -> col(index).toBinder()
}

private fun String.toBinder(): Identifier.Symbol = ast {
    // Every binder preserves case
    identifierSymbol(this@toBinder, Identifier.CaseSensitivity.SENSITIVE)
}

private fun Identifier.toBinder(): Identifier.Symbol = when (this@toBinder) {
    is Identifier.Qualified -> when (steps.isEmpty()) {
        true -> root.symbol.toBinder()
        else -> steps.last().symbol.toBinder()
    }
    is Identifier.Symbol -> symbol.toBinder()
}

@OptIn(PartiQLValueExperimental::class)
private fun Expr.Path.toBinder(index: Int): Identifier.Symbol {
    if (steps.isEmpty()) return root.toBinder(index)
    return when (val last = steps.last()) {
        is Expr.Path.Step.Symbol -> last.symbol.toBinder()
        is Expr.Path.Step.Index -> {
            val k = last.key
            if (k is Expr.Lit && k.value is StringValue) {
                (k.value as StringValue).value!!.toBinder()
            } else {
                col(index).toBinder()
            }
        }
        else -> col(index).toBinder()
    }
}
