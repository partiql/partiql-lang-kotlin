package org.partiql.planner.internal.helpers

import org.partiql.ast.Ast.identifier
import org.partiql.ast.Identifier
import org.partiql.ast.IdentifierChain
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

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
internal fun Expr.toBinder(index: () -> Int): Identifier = when (this) {
    is ExprVarRef -> this.identifierChain.toBinder()
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
internal fun Expr.toBinder(index: Int): Identifier = toBinder { index }

private fun String.toBinder(): Identifier =
    // Every binder preserves case
    identifier(this@toBinder, true)

private fun IdentifierChain.toBinder(): Identifier {
    if (next == null) return root.symbol.toBinder()
    var cur = next
    var prev = cur
    while (cur != null) {
        prev = cur
        cur = cur.next
    }
    return prev!!.root.symbol.toBinder()
}

private fun Identifier.toBinder(): Identifier = symbol.toBinder()

@OptIn(PartiQLValueExperimental::class)
private fun ExprPath.toBinder(index: () -> Int): Identifier {
    if (next == null) return root.toBinder(index)
    var cur = next
    var prev = next
    while (cur != null) {
        prev = cur
        cur = cur.next
    }
    return when (prev) {
        is PathStep.Field -> prev.field.toBinder()
        is PathStep.Element -> {
            val k = prev.element
            if (k is ExprLit && k.value is StringValue) {
                (k.value as StringValue).value!!.toBinder()
            } else {
                col(index).toBinder()
            }
        }
        else -> col(index).toBinder()
    }
}
