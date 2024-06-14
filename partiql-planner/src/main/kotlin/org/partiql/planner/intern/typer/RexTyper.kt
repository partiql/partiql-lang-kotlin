package org.partiql.planner.intern.typer

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.intern.validate.SqlScope
import org.partiql.planner.intern.validate.SqlValidator
import org.partiql.planner.intern.validate.Strategy
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.util.PlanRewriter
import org.partiql.types.StaticType

/**
 * Types a PartiQL expression tree. For now, we ignore the pre-existing type. We assume all existing types
 * are simply the `any`, so we keep the new type. Ideally we can programmatically calculate the most specific type.
 *
 * TODOs
 *  - Remove strategy from RexTyper and place in the unresolved variable.
 */
internal class RexTyper(
    private val validator: SqlValidator,
    private val types: SqlTypes<StaticType>,
    private val scope: SqlScope,
    private val strategy: Strategy,
) : PlanRewriter<StaticType>() {

    fun validate(rex: Rex): Rex = visitRex(rex, rex.type)

    /**
     * TODO merge into SqlAnalyzer once IR is updated.
     */
    private fun analyze(rel: Rel, bindings: SqlScope): Rel {
        return RelTyper(validator, types, bindings).visitRel(rel, EMPTY)
    }

    override fun visitRex(node: Rex, ctx: StaticType): Rex = visitRexOp(node.op, node.type) as Rex

    // PLACEHOLDER

    companion object {

        /**
         * Default EMPTY output relation type.
         */
        @JvmStatic
        private val EMPTY = Rel.Type(emptyList(), emptySet())
    }
}
