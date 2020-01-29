package org.partiql.examples

import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.*
import org.partiql.lang.eval.*
import java.io.PrintStream

/** Demonstrates how to supply a global variable whose value is lazily materialized. */
class EvaluationWithLazyBindings(out: PrintStream) : Example(out) {
    val ion = IonSystemBuilder.standard().build()
    val pipeline = CompilerPipeline.standard(ion)

    override fun run() {
        // Compile a simple SELECT query
        val query = "SELECT p.name AS kitten_id FROM pets AS p WHERE age >= 4"
        print("PartiQL query:", query)
        val e = pipeline.compile(query)

        // The global bindings
        val ionText = """[ { name: "Nibbler", age: 2 }, { name: "Hobbes", age: 6 } ]"""
        val globalVariables = Bindings.buildLazyBindings<ExprValue> {
            addBinding("pets") {
                // The first time "pets" is encountered during query evaluation this closure will be
                // invoked to obtain its value, which will then be cached for later use.
                // "pets" is the PartiQL equivalent of a a "table" (actually an Ion list of structs)
                pipeline.valueFactory.newFromIonValue(
                    ion.singleValue("""[ { name: "Nibbler", age: 2 }, { name: "Hobbes", age: 6 } ]"""))
            }
        }
        print("global variables:", "pets => $ionText")

        // Create session containing [globalVariables].
        val session = EvaluationSession.build { globals(globalVariables) }

        // Evaluate the query using the session.
        val result = e.eval(session)
        print("result:", result)
    }
}