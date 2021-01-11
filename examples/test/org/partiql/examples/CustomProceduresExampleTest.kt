package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class CustomProceduresExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = CustomProceduresExample(out)

    override val expected = """
        |Initial global session bindings:
        |Crew 1:
        |    [{'name': 'Neil', 'mass': 80.5}, {'name': 'Buzz', 'mass': 72.3}, {'name': 'Michael', 'mass': 89.9}]
        |Crew 2:
        |    [{'name': 'James', 'mass': 77.1}, {'name': 'Spock', 'mass': 81.6}]
        |Number of calculated moon weights:
        |    3
        |Updated global session bindings:
        |Crew 1:
        |    [{'name': 'Neil', 'mass': 80.5, 'moonWeight': 13.3}, {'name': 'Buzz', 'mass': 72.3, 'moonWeight': 12.0}, {'name': 'Michael', 'mass': 89.9, 'moonWeight': 14.9}]
        |Crew 2:
        |    [{'name': 'James', 'mass': 77.1}, {'name': 'Spock', 'mass': 81.6}]
        |
    """.trimMargin()
}
