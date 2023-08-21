package org.partiql.transpiler.sql

import org.partiql.ast.builder.ExprSfwBuilder
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.visitor.PlanBaseVisitor

/**
 * This class transforms a relational expression tree to a PartiQL [Expr.SFW].
 *
 * !!!
 * This is naive and simple.
 * This only targets the basic SFW; so we assume the Plan of the form: SCAN -> FILTER -> PROJECT.
 * !!!
 *
 * This will require non-trivial rework to handle arbitrary plans.
 * See https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/rel/rel2sql/RelToSqlConverter.java
 */
open class RelToSql(private val parent: RexToSql) : PlanBaseVisitor<ExprSfwBuilder, Rel.Type?>() {

    /**
     * This MUST return a mutable SFW builder because we'll need to inspect and/or replace the SELECT.
     */
    public fun apply(rel: Rel): ExprSfwBuilder {
        // descend
        val sfw = visitRel(rel, null)
        return sfw
    }

    override fun defaultReturn(node: PlanNode, ctx: Rel.Type?) = TODO("Cannot translate relation expression $node")

    /**
     * Pass along the RelType.
     */
    override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type)

    // /**
    //  * Logical Scan -> FROM Clause
    //  */
    // override fun visitRelOpScan(
    //     node: Rel.Op.Scan,
    //     ctx: Rel.Type?,
    // ): ExprSfwBuilder {
    //     val sfw = ExprSfwBuilder()
    //     sfw.from = ast {
    //         fromValue {
    //             expr = parent.visitRex(node.value, Unit)
    //             type = From.Value.Type.SCAN
    //             asAlias = node.alias?.let { id(it) }
    //             atAlias = node.at?.let { id(it) }
    //             byAlias = node.by?.let { id(it) }
    //         }
    //     }
    //     return sfw
    // }
    //
    // /**
    //  * Unpivot Scan -> FROM Clause
    //  */
    // override fun visitRelOpUnpivot(
    //     node: Rel.Op.Unpivot,
    //     ctx: Rel.Type?,
    // ): ExprSfwBuilder {
    //     val sfw = ExprSfwBuilder()
    //     sfw.from = ast {
    //         fromValue {
    //             expr = parent.visitRex(node.value, Unit)
    //             type = From.Value.Type.UNPIVOT
    //             asAlias = node.alias?.let { id(it) }
    //             atAlias = node.at?.let { id(it) }
    //             byAlias = node.by?.let { id(it) }
    //         }
    //     }
    //     return sfw
    // }
    //
    // /**
    //  * Filter -> WHERE or HAVING Clause
    //  *
    //  * This is where we make the naive assumption that this is a simple SELECT-FROM-WHERE.
    //  */
    // override fun visitRelOpFilter(
    //     node: Rel.Op.Filter,
    //     ctx: Rel.Type?,
    // ): ExprSfwBuilder {
    //     val sfw = visitRelOp(node.input, ctx)
    //     sfw.where = parent.visitRex(node.condition, Unit)
    //     return sfw
    // }
    //
    // /**
    //  * Project -> SELECT Clause
    //  */
    // override fun visitRelOpProject(
    //     node: Rel.Op.Project,
    //     ctx: Rel.Type?,
    // ): ExprSfwBuilder {
    //     val sfw = visitRelOp(node.input, ctx)
    //     sfw.select = ast {
    //         // SQL SELECT-PROJECT
    //         selectProject {
    //             for (b in node.bindings) {
    //                 items += selectProjectItemExpression {
    //                     expr = parent.visitRex(b.value, Unit)
    //                     asAlias = identifierSymbol(b.name, Identifier.CaseSensitivity.SENSITIVE)
    //                 }
    //             }
    //         }
    //     }
    //     return sfw
    // }
    //
    // // Helpers
    //
    // private fun id(symbol: String): Identifier.Symbol =
    //     Ast.identifierSymbol(symbol, Identifier.CaseSensitivity.SENSITIVE)
}
