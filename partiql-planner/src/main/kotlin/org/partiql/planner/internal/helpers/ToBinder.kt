package org.partiql.planner.internal.helpers

import org.partiql.ast.Ast.identifierSimple
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep

private val col = { index: () -> Int -> "_${index()}" }

/**
 * Produces a "binder" (AS alias) for an expression following the given rules:
 *
 *  1. If item is an id, use the last symbol
 *  2. If item is a path with a final symbol step, use the symbol — else 4
 *  3. If item is a cast, use the value name
 *  4. Else, use item index with prefix _
 *
 *  See https://github.com/partiql/partiql-lang-kotlin/issues/1122
 */
internal fun Expr.toBinder(index: () -> Int): Simple = when (this) {
    is ExprVarRef -> this.identifier.toBinder()
    is ExprPath -> this.toBinder(index)
    is ExprCast -> this.value.toBinder(index)
    is ExprSessionAttribute -> this.sessionAttribute.name().uppercase().toBinder()
    else -> col(index).toBinder()
}

/**
 * Simple toBinder that uses an int literal rather than a closure.
 *
 * @param index
 * @return
 */
internal fun Expr.toBinder(index: Int): Simple = toBinder { index }

private fun String.toBinder(): Simple =
    // Every binder preserves case
    identifierSimple(this@toBinder, true)

private fun Identifier.toBinder(): Simple = this.identifier.toBinder()

private fun Simple.toBinder(): Simple = text.toBinder()

private fun ExprPath.toBinder(index: () -> Int): Simple {
    if (steps.isEmpty()) return root.toBinder(index)
    return when (val last = steps.last()) {
        is PathStep.Field -> last.field.toBinder()
        is PathStep.Element -> {
            val k = last.element
            if (k is ExprLit && k.lit.code() == Literal.STRING) {
                k.lit.stringValue().toBinder()
            } else {
                col(index).toBinder()
            }
        }
        else -> col(index).toBinder()
    }
}
