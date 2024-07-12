package org.partiql.planner.internal.utils

import org.partiql.ast.Binder
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.ast
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
public fun Expr.toBinder(index: () -> Int): Binder = when (this) {
    is Expr.Var -> this.identifier.toBinder()
    is Expr.Path -> this.toBinder(index)
    is Expr.Cast -> this.value.toBinder(index)
    is Expr.SessionAttribute -> this.attribute.name.uppercase().toBinder()
    else -> col(index).toBinder()
}

/**
 * Simple toBinder that uses an int literal rather than a closure.
 *
 * @param index
 * @return
 */
internal fun Expr.toBinder(index: Int): Binder = toBinder { index }

// Conscious Decision,
// Identifier normalization will be the first step of AST normalization.
// Binder generation then will always be delimited:
// I.e., FROM bAr
// UPPER -> FROM bAr -> FROM "BAR" -> FROM "BAR" AS "BAR"
// DOWN -> FROM bAr -> FROM "bar" -> FROM "bar" AS "bar"
// EXACT -> FROM bAr -> FROM "bAr" -> FROM "bAr" AS "bAr"
// NULL -> FROM bAr -> FROM bAr -> FROM bAr AS "bAr"
private fun String.toBinder(): Binder = ast {
    binder(this@toBinder, false)
}

private fun Identifier.toBinder(): Binder = when (this@toBinder) {
    is Identifier.Qualified -> when (steps.isEmpty()) {
        true -> root.symbol.toBinder()
        else -> steps.last().symbol.toBinder()
    }
    is Identifier.Symbol -> symbol.toBinder()
}

@OptIn(PartiQLValueExperimental::class)
private fun Expr.Path.toBinder(index: () -> Int): Binder {
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
