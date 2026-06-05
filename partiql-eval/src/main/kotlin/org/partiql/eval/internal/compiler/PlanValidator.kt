package org.partiql.eval.internal.compiler

import org.partiql.plan.Action
import org.partiql.plan.Operand
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.plan.rex.RexTable
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException

/**
 * Validates that a plan uses ref-based table nodes (no embedded Table objects).
 * Functions and aggregates are allowed to be embedded (they are thread-safe).
 * Throws [PRuntimeException] if any embedded Table node is found.
 */
internal object PlanValidator : OperatorVisitor<Unit, Unit> {

    fun validate(plan: Plan) {
        val action = plan.action
        if (action is Action.Query) {
            action.rex.accept(this, Unit)
        }
    }

    override fun defaultReturn(operator: Operator, ctx: Unit) {}

    override fun defaultVisit(operator: Operator, ctx: Unit) {
        for (operand: Operand in operator.operands) {
            for (op: Operator in operand) {
                op.accept(this, Unit)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun visitTable(rex: RexTable, ctx: Unit) {
        throw invalid("Plan contains embedded Table object. Use PartiQLPlanner.builder().useRefs() to produce ref-based plans.")
    }

    private fun invalid(message: String): PRuntimeException {
        val error = PError.INTERNAL_ERROR(PErrorKind.COMPILATION(), null, IllegalStateException(message))
        return PRuntimeException(error)
    }
}
