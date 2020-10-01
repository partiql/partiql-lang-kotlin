package org.partiql.lang.eval

import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.Path
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.domains.PartiqlAst


/**
 * Determines an appropriate column name for the given [ExprNode].
 *
 * If [this] is a [PartiqlAst.Expr.Id], returns the name of the variable.
 *
 * If [this] is a [PartiqlAst.Expr.Path], invokes [PartiqlAst.Expr.Path.extractColumnAlias] to determine the alias.
 *
 * Otherwise, returns the column index prefixed with `_`.
 */
fun PartiqlAst.Expr.extractColumnAlias(idx: Int): String =
    when (this) {
        is PartiqlAst.Expr.Id -> this.name.text
        is PartiqlAst.Expr.Path -> {
            this.extractColumnAlias(idx)
        }
        else -> syntheticColumnName(idx)
    }

/**
 * Returns the name of the last component if it is a string literal, otherwise returns the
 * column index prefixed with `_`.
 */
fun PartiqlAst.Expr.Path.extractColumnAlias(idx: Int): String {
    return when (val nameOrigin = this.steps.last()) {
        is PartiqlAst.PathStep.PathExpr -> {
            val maybeLiteral = nameOrigin.index
            when {
                maybeLiteral is PartiqlAst.Expr.Lit && maybeLiteral.value is TextElement -> maybeLiteral.value.textValue
                else -> syntheticColumnName(idx)
            }
        }
        else -> syntheticColumnName(idx)
    }
}

