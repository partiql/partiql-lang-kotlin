package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst

/** Coerce each SQL-style SELECT subquery to single value, if it occurs in a context that expects a single value -- as
 *  prescribed in SQL for scalar-expecting contexts and as outlined in Chapter 9 of the PartiQL specification.
 *  The coercion is done by wrapping each eligible SELECT in a call to the COLL_TO_SCALAR built-in.
 *
 *  The implementation is somewhat clever, dealing with the problem that not every SELECT needs to be coerced,
 *  but only a SELECT subquery in an appropriate context, while its coercion details are context-dependent.
 */
class SubqueryCoercionVisitorTransform : VisitorTransformBase() {

    /** When an Expr E is reached during the visitor's traversal,
     * - First, perform the visitor's transformation recursively on E's components.
     *   This will coerce eligible SELECTs deep in each E's subexpression,
     *   but won't coerce any E's direct subexpression itself, even if it is a SELECT.
     * - Then, treat E as a context for a possible SELECT subquery.
     *   That is, detect each possible subexpression of E and, when the context is right
     *   and the subexpression is an SQL-style SELECT, coerce it.
     * With this, a top-level expression is never coerced, whether it is a SELECT or not.
     */
    override fun transformExpr(node: PartiqlAst.Expr): PartiqlAst.Expr {
        val n = super.transformExpr(node)
        return when (n) {
            is PartiqlAst.Expr.Missing -> n
            is PartiqlAst.Expr.Lit -> n
            is PartiqlAst.Expr.Id -> n
            is PartiqlAst.Expr.Parameter -> n

            is PartiqlAst.Expr.Not -> n.copy(expr = coerceToSingle(n.expr))
            is PartiqlAst.Expr.Pos -> n.copy(expr = coerceToSingle(n.expr))
            is PartiqlAst.Expr.Neg -> n.copy(expr = coerceToSingle(n.expr))

            is PartiqlAst.Expr.Plus -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Minus -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Times -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Divide -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Modulo -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Concat -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.And -> n.copy(operands = n.operands.map { coerceToSingle(it) })
            is PartiqlAst.Expr.Or -> n.copy(operands = n.operands.map { coerceToSingle(it) })

            is PartiqlAst.Expr.Eq -> n.copy(operands = coerceInComparisonOps(n.operands))
            is PartiqlAst.Expr.Ne -> n.copy(operands = coerceInComparisonOps(n.operands))
            is PartiqlAst.Expr.Gt -> n.copy(operands = coerceInComparisonOps(n.operands))
            is PartiqlAst.Expr.Gte -> n.copy(operands = coerceInComparisonOps(n.operands))
            is PartiqlAst.Expr.Lt -> n.copy(operands = coerceInComparisonOps(n.operands))
            is PartiqlAst.Expr.Lte -> n.copy(operands = coerceInComparisonOps(n.operands))

            is PartiqlAst.Expr.Like -> n.copy(value = coerceToSingle(n.value), pattern = coerceToSingle(n.pattern))
            is PartiqlAst.Expr.Between -> n.copy(value = coerceToSingle(n.value), from = coerceToSingle(n.from), to = coerceToSingle(n.to))

            is PartiqlAst.Expr.InCollection -> toSingleInInCollection(n)

            // TODO Revisit the following expression forms, where this implementation does not coerce most potential SELECT subqueries,
            // being at odds with the literal letter of the Ch 9 of the PartiQL specification.
            // Some of these are clear-cut cases, others need to be considered and decided upon.
            is PartiqlAst.Expr.IsType -> n
            is PartiqlAst.Expr.SimpleCase -> n
            is PartiqlAst.Expr.SearchedCase -> n
            is PartiqlAst.Expr.Struct -> n
            is PartiqlAst.Expr.Bag -> n
            is PartiqlAst.Expr.List -> n
            is PartiqlAst.Expr.Sexp -> n
            is PartiqlAst.Expr.Date -> n
            is PartiqlAst.Expr.LitTime -> n
            is PartiqlAst.Expr.BagOp -> n

            is PartiqlAst.Expr.GraphMatch -> n.copy(expr = coerceToSingle(n.expr))

            // ... continuing the expressions with so far un-coerced subqueries
            is PartiqlAst.Expr.Path -> n
            is PartiqlAst.Expr.Call -> n
            is PartiqlAst.Expr.CallAgg -> n
            is PartiqlAst.Expr.CallWindow -> n
            is PartiqlAst.Expr.Cast -> n
            is PartiqlAst.Expr.CanCast -> n
            is PartiqlAst.Expr.CanLosslessCast -> n
            is PartiqlAst.Expr.NullIf -> n
            is PartiqlAst.Expr.Coalesce -> n

            is PartiqlAst.Expr.Select ->
                n.copy(
                    setq = n.setq,
                    project = toSingleInProject(n.project),
                    from = n.from, // per SQL, don't coerce subqueries that are data sources in FROM
                    fromLet = n.fromLet, // LET is PartiQL-specific, so allow binding to a non-scalar
                    where = n.where?.let { coerceToSingle(it) },
                    group = n.group?.let { toSingleInGroup(it) },
                    having = n.having?.let { coerceToSingle(it) },
                    order = n.order?.let { toSingleInOrderby(it) },
                    limit = n.limit?.let { coerceToSingle(it) },
                    offset = n.offset?.let { coerceToSingle(it) }
                )
        }
    }

    /**  Whenever the expression is an SQL-style select, wrap it with a singleton coercion. */
    private fun coerceToSingle(e: PartiqlAst.Expr): PartiqlAst.Expr =
        when (e) {
            is PartiqlAst.Expr.Select ->
                if ((e.project is PartiqlAst.Projection.ProjectStar) || (e.project is PartiqlAst.Projection.ProjectList)) {
                    PartiqlAst.build { call("coll_to_scalar", e) }
                } else e
            else -> e
        }

    private fun coerceToArray(e: PartiqlAst.Expr): PartiqlAst.Expr =
        e // TODO: actual to-array coercion, like toSingle above

    /** Coerce the two operands of a comparison operation (<, =, ...),
     *  considering each as part of the context for the other, as prescribed in SQL. */
    private fun coerceInComparisonOps(operands: List<PartiqlAst.Expr>): List<PartiqlAst.Expr> {
        fun isArrayLiteral(x: PartiqlAst.Expr): Boolean = x is PartiqlAst.Expr.List
        val lhs = operands[0]
        val rhs = operands[1]
        val rest = operands.takeLast(operands.size - 2) // these are useless operands, but the AST carries them, so here we go
        return when {
            isArrayLiteral(lhs) && isArrayLiteral(rhs) -> operands
            isArrayLiteral(lhs) -> listOf(lhs, coerceToArray(rhs)) + rest
            isArrayLiteral(rhs) -> listOf(coerceToArray(lhs), rhs) + rest
            else -> listOf(coerceToSingle(lhs), coerceToSingle(rhs)) + rest
        }
    }

    /** Only coerce the element(lhs) side of `<lhs> IN <rhs>` */
    private fun toSingleInInCollection(n: PartiqlAst.Expr.InCollection): PartiqlAst.Expr.InCollection {
        val ops = n.operands
        // only the first two operands are meaningful for IN, but the AST supports multiple, so here we go
        return n.copy(operands = listOf(coerceToSingle(ops[0]), ops[1]) + ops.takeLast(ops.size - 2))
    }

    private fun toSingleInProject(p: PartiqlAst.Projection): PartiqlAst.Projection =
        when (p) {
            is PartiqlAst.Projection.ProjectList ->
                p.copy(
                    projectItems = p.projectItems.map { i ->
                        when (i) {
                            is PartiqlAst.ProjectItem.ProjectExpr -> i.copy(expr = coerceToSingle(i.expr))
                            else -> i
                        }
                    }
                )
            else -> p
        }

    private fun toSingleInGroup(g: PartiqlAst.GroupBy): PartiqlAst.GroupBy =
        g.copy(keyList = g.keyList.copy(keys = g.keyList.keys.map { k -> k.copy(expr = coerceToSingle(k.expr)) }))

    private fun toSingleInOrderby(b: PartiqlAst.OrderBy): PartiqlAst.OrderBy =
        b.copy(sortSpecs = b.sortSpecs.map { s -> s.copy(expr = coerceToSingle(s.expr)) })
}
