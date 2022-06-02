package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class EvaluationWithBindingsTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = EvaluationWithBindings(out)

    override val expected = """
        |PartiQL query:
        |    'Hello, ' || user_name
        |global variables:
        |    user_name => 'Homer Simpson'
        |result
        |    'Hello, Homer Simpson'
        |
    """.trimMargin()
}
