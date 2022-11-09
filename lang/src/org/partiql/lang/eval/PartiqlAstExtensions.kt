package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlAst
import org.partiql.pig.runtime.DomainNode

/**
 * Determines an appropriate column name for the given [PartiqlAst.Expr].
 *
 * If [this] is a [PartiqlAst.Expr.Id], returns the name of the variable.
 *
 * If [this] is a [PartiqlAst.Expr.Path], invokes [PartiqlAst.Expr.Path.extractColumnAlias] to determine the alias.
 *
 * If [this] is a [PartiqlAst.Expr.Cast], the column alias is the name of the [PartiqlAst.Expr.Cast.value] to be `CAST`.
 *
 * Otherwise, returns the column index prefixed with `_`.
 */
fun PartiqlAst.Expr.extractColumnAlias(idx: Int): String =
    when (this) {
        is PartiqlAst.Expr.Id -> this.name.text
        is PartiqlAst.Expr.Path -> {
            this.extractColumnAlias(idx)
        }
        is PartiqlAst.Expr.Cast -> {
            this.value.extractColumnAlias(idx)
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

/**
 * Returns the starting [SourceLocationMeta] found through walking through all nodes of [this] [PartiqlAst.Expr].
 * Starting is defined to be the [SourceLocationMeta] with the lowest [SourceLocationMeta.lineNum] and in the event of
 * a tie, the lowest [SourceLocationMeta.charOffset].
 */
internal fun PartiqlAst.Expr.getStartingSourceLocationMeta(): SourceLocationMeta {
    val visitorFold = object : PartiqlAst.VisitorFold<SourceLocationMeta>() {
        override fun visitMetas(node: MetaContainer, accumulator: SourceLocationMeta): SourceLocationMeta {
            val nodeSourceLocation = node.sourceLocation
            return nodeSourceLocation?.takeIf {
                (
                    nodeSourceLocation.lineNum < accumulator.lineNum ||
                        (nodeSourceLocation.lineNum == accumulator.lineNum && nodeSourceLocation.charOffset < accumulator.charOffset)
                    )
            } ?: accumulator
        }
    }
    return visitorFold.walkExpr(this, SourceLocationMeta(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
}
