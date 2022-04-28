package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode

/**
 * Represents an abstract pipeline (either [org.partiql.lang.CompilerPipeline] or
 * [org.partiql.lang.planner.PlannerPipeline]) so that [PipelineEvaluatorTestAdapter] can work with either.
 *
 * Includes only those properties and methods that are required for testing purposes.
 */
interface AbstractPipeline {
    val typingMode: TypingMode
    fun evaluate(query: String): ExprValue
}
