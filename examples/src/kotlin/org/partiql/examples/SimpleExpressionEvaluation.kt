package org.partiql.examples

import org.partiql.examples.util.Example
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import java.io.PrintStream

/** Demonstrates how to compile and evaluate a simple expression. */
class SimpleExpressionEvaluation(out: PrintStream) : Example(out) {
    val pipeline = CompilerPipeline.standard()

    override fun run() {
        // An EvaluationSession holds context needed for evaluation of the compiled query.
        val session = EvaluationSession.standard()

        // Compiles a simple expression.  This only needs to be performed once per query.
        val query = "1 + 1"
        print("PartiQL query:", query)
        val e = pipeline.compile(query)

        // Evaluate the compiled expression.  This can be executed repeatedly.
        val result = e.eval(session)
        print("result", result.toString())
    }
}
