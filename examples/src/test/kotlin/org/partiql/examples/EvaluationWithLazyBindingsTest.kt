package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class EvaluationWithLazyBindingsTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = EvaluationWithLazyBindings(out)

    override val expected = """
        |PartiQL query:
        |    SELECT p.name AS kitten_id FROM pets AS p WHERE age >= 4
        |global variables:
        |    pets => [ { name: "Nibbler", age: 2 }, { name: "Hobbes", age: 6 } ]
        |result:
        |    <<
        |      {
        |        'kitten_id': 'Hobbes'
        |      }
        |    >>
        |
    """.trimMargin()
}
