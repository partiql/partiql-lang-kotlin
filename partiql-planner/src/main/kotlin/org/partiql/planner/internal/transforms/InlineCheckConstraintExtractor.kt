package org.partiql.planner.internal.transforms
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.types.shape.PShape
import org.partiql.types.shape.trait.ConstraintTrait
import org.partiql.types.shape.trait.RangeTrait
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Lowers an inline check constraint to PShape when applicable.
 *
 * For example,
 *
 * `Foo INT2 CHECK(Foo >= 0 AND FOO <=10)` will be lowered to a PShape with trait range(0,10).
 *
 * We can extend the lowering capability in the future if we want to, but for now,
 * it only attempts to lower if the expression is
 * 1. Simple expression in which the operator is either GTE or LTE.
 * 2. Multiple simple expressions chained by and operator.
 */
internal object InlineCheckConstraintExtractor : PlanBaseVisitor<PShape, PShape>() {
    // Unable to lower the check constraint to PShape
    override fun defaultReturn(node: PlanNode, ctx: PShape): PShape = ctx

    override fun visitStatementDDLConstraintCheck(node: Statement.DDL.Constraint.Check, ctx: PShape): PShape {
        val lowered = visitRex(node.expression, ctx)
        // No lowering happened, then wrap the PShape with a generic Constraint trait
        return if (lowered == ctx) {
            ConstraintTrait(ctx, node.sql)
        } else lowered
    }

    override fun visitRex(node: Rex, ctx: PShape): PShape {
        return node.op.accept(this, ctx)
    }

    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: PShape): PShape {
        return when (node.fn.name) {
            "gte" -> getRhsAsNumericOrNull(node.args[1])?.let {
                RangeTrait(ctx, it, null)
            } ?: ctx
            "lte" -> getRhsAsNumericOrNull(node.args[1])?.let {
                RangeTrait(ctx, null, it)
            } ?: ctx
            "and" -> handleAnd(node, ctx)
            else -> super.visitRexOpCall(node, ctx)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun getRhsAsNumericOrNull(rex: Rex) =
        when (val op = rex.op) {
            is Rex.Op.Lit -> {
                when (val v = op.value) {
                    is NumericValue<*> -> v.value
                    else -> null
                }
            }
            else -> null
        }

    private fun handleAnd(node: Rex.Op.Call.Static, ctx: PShape): PShape {
        val lhs = node.args.first().accept(this, ctx)
        // No lowering happened for lhs, do not attempt to lower the right-hand side
        if (lhs == ctx) { return ctx }
        val rhs = node.args[1].accept(this, lhs)
        return rhs
    }
}
