package org.partiql.lang.eval.physical.operators

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState

/**
 * Evaluates a PartiQL expression returning an [ExprValue].
 *
 * [BindingsExpr] implementations need a mechanism to evaluate such expressions, and said mechanism should
 * avoid exposing implementation details (i.e. [org.partiql.lang.eval.physical.PhysicalPlanThunk]) of the evaluator.
 * This implementation accomplishes that and is intended as a publicly usable API that is supported long term.
 */
interface ValueExpr {
    /** Evaluates the expression. */
    operator fun invoke(state: EvaluatorState): ExprValue

    /** Provides the source location (line & column) of the expression, for error reporting purposes. */
    val sourceLocation: SourceLocationMeta?
}

/** Convenience constructor for [ValueExpr]. */
internal inline fun valueExpr(sourceLocation: SourceLocationMeta?, crossinline invoke: (EvaluatorState) -> ExprValue) =
    object : ValueExpr {
        override fun invoke(state: EvaluatorState): ExprValue = invoke(state)
        override val sourceLocation: SourceLocationMeta? get() = sourceLocation
    }
