package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.toBindingCase

/**
 * Determines an appropriate column name for the given [PartiqlAst.Expr].
 *
 * If [this] is a [PartiqlAst.Expr.Id], returns the name of the variable.
 *
 * If [this] is a [PartiqlAst.Expr.Path], invokes [PartiqlAst.Expr.Path.extractColumnAlias] to determine the alias.
 *
 * If [this] is a [PartiqlAst.Expr.Cast], the column alias is the same as would be given to the [PartiqlAst.Expr.Cast.value] to be `CAST`.
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

/** Collect variables bound in the FROM and LET clauses of a SELECT query.
 *  Assumes that the expression has been normalized/desugared with [VisitorTransforms.basicVisitorTransforms].
 */
internal fun PartiqlAst.Expr.Select.boundVariables(): Set<String> {
    val selectExpr = this

    // Get all the FROM source aliases and LET bindings for binding error checks
    val fold = object : PartiqlAst.VisitorFold<Set<String>>() {
        /** Store all the visited FROM source aliases in the accumulator */
        override fun visitFromSourceScan(node: PartiqlAst.FromSource.Scan, accumulator: Set<String>): Set<String> {
            val aliases = listOfNotNull(node.asAlias?.text, node.atAlias?.text, node.byAlias?.text)
            return accumulator + aliases
        }

        override fun visitLetBinding(node: PartiqlAst.LetBinding, accumulator: Set<String>): Set<String> {
            val aliases = listOfNotNull(node.name.text)
            return accumulator + aliases
        }

        /** Prevents visitor from recursing into nested select statements */
        override fun walkExprSelect(node: PartiqlAst.Expr.Select, accumulator: Set<String>): Set<String> {
            return accumulator
        }
    }

    return fold.walkFromSource(selectExpr.from, emptySet())
        .union(selectExpr.fromLet?.let { fold.walkLet(it, emptySet()) } ?: emptySet())
}

/** Get the [BindingName] that is equivalent to this reference identifier (including case-sensitivity). */
internal fun PartiqlAst.Expr.Id.toBindingName(): BindingName =
    BindingName(this.name.text, this.case.toBindingCase())

/** Free variables in an expression.
 *  A variable (a PartiqlAst.Expr.Id) is free, unless it is in the scope of a same-named alias introduced
 *  in FROM, LET, or GROUP BY clauses.
 *  Assumes that the expression has been normalized/desugared with [VisitorTransforms.basicVisitorTransforms].
 */
internal fun PartiqlAst.Expr.freeVariables(): Set<String> {
    val visitorFold = object : PartiqlAst.VisitorFold<Set<String>>() {
        // The normal mode of operation is walk through the most AST nodes just carrying the accumulator set along.

        // When a variable reference (a PartiqlAst.Expr.Id) is encountered, add it to the accumulator.
        override fun walkExprId(node: PartiqlAst.Expr.Id, accumulator: Set<String>): Set<String> {
            return accumulator + node.name.text
        }

        // The invariant of each walkExprXxx(node: Xxx, accumulator) call
        // is to add all *free* variables of `node` expression to `accumulator`.
        // The default implementations work for all expressions except SELECT.

        // Processing the SELECT involves taking care that variable occurrences bound by FROM, LET, and GROUP BY clauses
        // do not end up in the result, if they are in the scope of those bindings.
        override fun walkExprSelect(node: PartiqlAst.Expr.Select, accumulator: Set<String>): Set<String> {
            var current = emptySet<String>()

            // SELECT clauses are processed in the reverse of evaluation order, since that's how variable scoping extends.

            // Note: in `SELECT e AS x ...`, free variables of `e` get into the result,
            // but `x` *does not*, due to `x` being a 'symbol', not an 'id' in the AST.
            // (For reference, 'x' *should not* count as either free or bound because it is not a variable,
            // it is a spec to create an attribute in a struct.)
            current += walkProjection(node.project, emptySet())

            node.limit?.let { current += walkExpr(it, emptySet()) }
            node.offset?.let { current += walkExpr(it, emptySet()) }

            node.order?.let { current += walkOrderBy(it, emptySet()) }

            node.having?.let { current += walkExpr(it, emptySet()) }

            node.group?.let {
                val freeInGroup = walkGroupBy(it, emptySet())
                val boundByGroup =
                    (it.groupAsAlias?.let { setOf(it.text) } ?: emptySet()) +
                        it.keyList.keys.flatMap { it.asAlias?.let { setOf(it.text) } ?: emptySet() }
                current = (current - boundByGroup) + freeInGroup
            }

            node.where?.let { current += walkExpr(it, emptySet()) }

            node.fromLet?.let {
                current = addFreeOfLets(it.letBindings, current)
            }

            current = addFreeOfFrom(node.from, current)

            return accumulator + current
        }

        // Accounts for situations when an earlier binder is referenced in a later FROM item, such as t in 2nd item in
        //   FROM Tbl as t, t as x
        // Such a reference should not be counted as free.
        private fun addFreeOfFrom(n: PartiqlAst.FromSource, accum: Set<String>): Set<String> =
            when (n) {
                is PartiqlAst.FromSource.Join -> {
                    val condVars = n.predicate?.let { walkExpr(it, emptySet()) } ?: emptySet()
                    val acc = addFreeOfFrom(n.right, accum + condVars)
                    addFreeOfFrom(n.left, acc)
                }
                is PartiqlAst.FromSource.Scan -> {
                    val binders = setOfNotNull(n.asAlias?.text, n.atAlias?.text, n.byAlias?.text)
                    (accum - binders) + walkExpr(n.expr, emptySet())
                }
                is PartiqlAst.FromSource.Unpivot -> {
                    val binders = setOfNotNull(n.asAlias?.text, n.atAlias?.text, n.byAlias?.text)
                    (accum - binders) + walkExpr(n.expr, emptySet())
                }
            }

        // Similarly to FROM, accounts for, e.g.,  LET expr as x, 2*x+1 as z   -- 2nd x is not free.
        private fun addFreeOfLets(ns: List<PartiqlAst.LetBinding>, accum: Set<String>): Set<String> =
            ns.foldRight(accum) { n, acc ->
                (acc - n.name.text) + walkExpr(n.expr, emptySet())
            }
    }

    return visitorFold.walkExpr(this, emptySet())
}
