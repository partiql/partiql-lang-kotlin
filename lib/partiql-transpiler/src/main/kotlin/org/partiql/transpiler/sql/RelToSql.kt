package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.builder.ExprSfwBuilder
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.visitor.PlanBaseVisitor

/**
 * This class transforms a relational expression tree to a PartiQL [Expr.SFW].
 *
 * !!! IMPORTANT !!!
 *
 * TODO This is naive and simple.
 * TODO This only targets the basic SFW; so we assume the Plan of the form: SCAN -> FILTER -> PROJECT.
 * TODO This will require non-trivial rework to handle arbitrary plans.
 * TODO See Calcite's RelToSql and SqlImplementor
 * TODO https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/rel/rel2sql/RelToSqlConverter.java
 *
 * !!! IMPORTANT !!!
 */
open class RelToSql(
    private val transform: SqlTransform,
) : PlanBaseVisitor<ExprSfwBuilder, Rel.Type?>() {

    /**
     * This MUST return a mutable SFW builder because we'll need to inspect and/or replace the SELECT.
     */
    public fun apply(rel: Rel): ExprSfwBuilder {
        assertClauses(rel)
        return visitRel(rel, null)
    }

    /**
     * TODO TEMPORARY — REMOVE ME, this could be made generic but honestly this is fine.
     */
    private fun assertClauses(rel: Rel) {
        val op1 = rel.op
        if (op1 !is Rel.Op.Project) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Project but found $op1")
        }
        val op2 = op1.input.op
        if (op2 is Rel.Op.Scan) {
            return // done
        }
        if (op2 !is Rel.Op.Filter) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Filter but found $op2")
        }
        val op3 = op2.input.op
        if (op3 !is Rel.Op.Scan) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Scan but found $op3")
        }
    }

    /**
     * Default behavior is considered unsupported.
     */
    override fun defaultReturn(node: PlanNode, ctx: Rel.Type?) =
        throw UnsupportedOperationException("Cannot translate rel $node")

    override fun defaultVisit(node: PlanNode, ctx: Rel.Type?) = defaultReturn(node, ctx)

    /**
     * Pass along the Rel.Type?.
     */
    override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type)

    /**
     * Logical Scan -> FROM Clause
     */
    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = ExprSfwBuilder()
        // validate scan type
        val type = ctx!!
        assert(type.schema.size == 1) { "Invalid SCAN schema, expected a single binding but found ${ctx.dump()}" }
        val rexToSql = RexToSql(transform, type.schema)
        // unpack to FROM clause
        sfw.from = Ast.create {
            fromValue(
                expr = rexToSql.apply(node.rex), // FROM <rex>,
                type = From.Value.Type.SCAN,
                asAlias = id(type.schema[0].name),
                atAlias = null,
                byAlias = null,
            )
        }
        return sfw
    }

    /**
     * Filter -> WHERE or HAVING Clause
     */
    // override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): ExprSfwBuilder {
    //     val sfw = visitRel(node.input, ctx)
    //     sfw.where = parent.visitRex(node.predicate, Unit)
    //     return sfw
    // }

    /**
     * Project -> SELECT Clause
     */
    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = visitRel(node.input, null)
        val rexToSql = RexToSql(transform, node.input.type.schema)
        val type = ctx!!
        assert(type.schema.size == node.projections.size) { "Malformed plan, relation output type does not match projections" }
        sfw.select = Ast.create {
            selectProject(
                items = node.projections.mapIndexed { i, rex ->
                    selectProjectItemExpression(
                        expr = rexToSql.apply(rex),
                        asAlias = id(type.schema[i].name),
                    )
                },
                setq = null,
            )
        }
        return sfw
    }

    private fun id(symbol: String): Identifier.Symbol = Ast.identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
    )

    private fun Rel.Type.dump(): String {
        if (this.schema.isEmpty()) return "< empty >"
        val pairs = this.schema.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }
}
