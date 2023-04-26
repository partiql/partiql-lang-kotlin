package org.partiql.examples

import org.partiql.examples.util.Example
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.GlobalsCheck
import java.io.PrintStream

/** Demonstrates how to supply a global variable to the session. */
class EvaluationWithBindings(out: PrintStream) : Example(out) {

    override fun run() {
        // This is the value of the global variable.
        val userName = "Homer Simpson"
        val usernameValue = ExprValue.newString(userName)

        // [Bindings.ofMap] can be used to construct a [Bindings] instance of
        // bindings with previously materialized values.
        val globals = mapOf("user_name" to usernameValue)
        val globalVariables = Bindings.ofMap(globals)
        // Include globalVariables when building the EvaluationSession.
        val session = EvaluationSession.build { globals(globalVariables) }

        // Compiles a simple expression containing a reference to a global variable.
        val pipeline = CompilerPipeline.standard(GlobalsCheck.of(session))
        val query = "'Hello, ' || user_name"
        print("PartiQL query:", query)
        val e = pipeline.compile(query)

        print("global variables:", globals)

        // Evaluate the compiled expression with the session containing the global variables.
        val result = e.eval(session)
        print("result", result.toString())
    }
}
