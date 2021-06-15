package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.checkThreadInterrupted

/**
 * Base-class for visitor transforms that provides additional `transform*` functions that outside of
 * the PIG-generated [PartiqlAst.VisitorTransform] class and adds a [Thread.interrupted] check
 * to [transformExpr].
 *
 * All transforms should derive from this class instead of [PartiqlAst.VisitorTransform] so that they can
 * be interrupted if they take a long time to process large ASTs.
 */
abstract class VisitorTransformBase : PartiqlAst.VisitorTransform() {

    override fun transformExpr(node: PartiqlAst.Expr): PartiqlAst.Expr {
        checkThreadInterrupted()
        return super.transformExpr(node)
    }

    /**
     * Transforms the [PartiqlAst.Expr.Select] expression following the PartiQL evaluation order. That is:
     *
     * 1. `FROM`
     * 2. `LET`
     * 3. `WHERE`
     * 4. `GROUP BY`
     * 5. `HAVING`
     * 6. *projection*
     * 7. `LIMIT`
     * 8. The metas.
     *
     * This differs from [transformExprSelect], which executes following the written order of clauses.
     */
    fun transformExprSelectEvaluationOrder(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val from = transformExprSelect_from(node)
        val fromLet = transformExprSelect_fromLet(node)
        val where = transformExprSelect_where(node)
        val group = transformExprSelect_group(node)
        val having = transformExprSelect_having(node)
        val setq = transformExprSelect_setq(node)
        val project = transformExprSelect_project(node)
        val order = transformExprSelect_order(node)
        val limit = transformExprSelect_limit(node)
        val metas = transformExprSelect_metas(node)
        return PartiqlAst.build {
            PartiqlAst.Expr.Select(
                setq = setq,
                project = project,
                from = from,
                fromLet = fromLet,
                where = where,
                group = group,
                having = having,
                order = order,
                limit = limit,
                metas = metas)
        }
    }

    /**
     * Transforms the [PartiqlAst.Statement.Dml] expression following the PartiQL evaluation order. That is:
     *
     * 1. `FROM`
     * 2. `WHERE`
     * 3. The DML operation
     * 4. The metas
     *
     * This differs from [transformStatementDml], which executes following the written order of clauses.
     */
    fun transformDataManipulationEvaluationOrder(node: PartiqlAst.Statement.Dml): PartiqlAst.Statement {
        val from = node.from?.let { transformFromSource(it) }
        val where = node.where?.let { transformStatementDml_where(node) }
        val dmlOperations = transformDmlOpList(node.operations)
        val returning = node.returning?.let { transformReturningExpr(it) }
        val metas = transformMetas(node.metas)

        return PartiqlAst.build {
            dml(
                dmlOperations,
                from,
                where,
                returning,
                metas)
        }
    }
}
