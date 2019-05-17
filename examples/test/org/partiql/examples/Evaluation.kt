package org.partiql.examples

import org.junit.*
import org.junit.Assert.*
import com.amazon.ion.system.*
import org.partiql.lang.*
import org.partiql.lang.eval.*

class Evaluation {
    private val ion = IonSystemBuilder.standard().build()
    private val pipeline = CompilerPipeline.standard(ion)

    /** Demonstrates how to compile and evaluate a simple expression. */
    @Test
    fun evaluateSimpleExpression() {
        // An EvaluationSession holds context needed for evaluation of the compiled query.
        val session = EvaluationSession.standard()

        // Compiles a simple expression.  This only needs to be performed once per query.
        val e = pipeline.compile("1 + 1")

        // Evaluate the compiled expression.  This can be executed repeatedly.
        val result = e.eval(session)

        // Verify the result
        assertEquals(2, result.numberValue().toInt())
    }

    /** Demonstrates how to supply a global variable to the session. */
    @Test
    fun evaluateExpressionWithMapBindings() {
        // Compiles a simple expression containing a reference to a global variable.
        val e = pipeline.compile("'Hello, ' || user_name")

        // This is the value of the global variable.
        val usernameValue = pipeline.valueFactory.newString("Homer Simpson")

        // [Bindings.ofMap] can be used to construct a [Bindings] instance of
        // bindings with previously materailized values.
        val globalVariables = Bindings.ofMap(mapOf("user_name" to usernameValue))

        // Include globalVariables when building the EvaluationSession.
        val session = EvaluationSession.build { globals(globalVariables) }

        // Evaluate the compiled expression with the session containing the global variables.
        val result = e.eval(session)

        //Verify the result.
        assertEquals("Hello, Homer Simpson", result.stringValue())
    }

    /** Demonstrates how to supply a global variable whose value is lazily materialized. */
    @Test
    fun evaluateExpressionWithLazyBindings() {
        // Compile a simple SELECT query
        val e = pipeline.compile("SELECT p.name AS kitten_id FROM pets AS p WHERE age >= 4")

        // The global bindings
        val globalVariables = Bindings.buildLazyBindings {
            addBinding("pets") {
                // The first time "pets" is encountered during query evaluation this closure will be
                // invoked to obtain its value, which will then be cached for later use.
                // "pets" is the PartiQL equivalent of a a "table" (actually an Ion list of structs)
                pipeline.valueFactory.newFromIonValue(
                    ion.singleValue("""[ { name: "Nibbler", age: 2 }, { name: "Hobbes", age: 6 } ]"""))
            }
        }

        // Create session containing [globalVariables].
        val session = EvaluationSession.build { globals(globalVariables) }

        // Evaluate the query using the session.
        val result = e.eval(session)

        // Results from SELECT queries always have type BAG which is like a list but without a guaranteed ordering.
        assertEquals(result.type, ExprValueType.BAG)

        // Only one kitten should match the criteria of the query.
        assertEquals(1, result.count())

        // Verify that the single result is as we'd expect.
        assertEquals(ion.singleValue(""" { kitten_id: "Hobbes" } """), result.first().ionValue)
    }
}