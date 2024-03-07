package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode

/**
 * Represents an abstract pipeline (one of [org.partiql.lang.CompilerPipeline],
 * [org.partiql.lang.compiler.PartiQLCompilerPipeline], or [org.partiql.lang.compiler.PartiQLCompilerPipelineAsync])
 * so that [PipelineEvaluatorTestAdapter] can work on any of them.
 *
 * Includes only those properties and methods that are required for testing purposes.
 */
interface AbstractPipeline {
    val typingMode: TypingMode
    fun evaluate(query: String): ExprValue
}
