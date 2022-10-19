package org.partiql.lang.planner.validators

import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.util.propertyValueMapOf

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct [PartiqlLogicalResolved.Visitor] so that all other visitors may assume that the AST at least
 * passed the checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 * - [org.partiql.lang.planner.transforms.LogicalToLogicalResolvedVisitorTransform]
 */
class PartiqlLogicalResolvedValidator : PartiqlLogicalResolved.Visitor() {
    /**
     * Quick validation step to make sure the indexes of any variables make sense.
     * It is unlikely that this check will ever fail, but if it does, it likely means there's a bug in
     * [org.partiql.lang.planner.transforms.VariableIdAllocator] or that the plan was malformed by other means.
     */
    override fun visitPlan(node: PartiqlLogicalResolved.Plan) {
        node.locals.forEachIndexed { idx, it ->
            if (it.registerIndex.value != idx.toLong()) {
                throw EvaluationException(
                    message = "Variable index must match ordinal position of variable",
                    errorCode = ErrorCode.INTERNAL_ERROR,
                    errorContext = propertyValueMapOf(),
                    internal = true
                )
            }
        }
        super.visitPlan(node)
    }
}
