package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class PreventJoinVisitorExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = PreventJoinVisitorExample(out)

    override val expected = """
        |PartiQL query without JOINs:
        |    SELECT foo FROM bar
        |Has joins:
        |    false
        |PartiQL query with JOINs:
        |    SELECT foo FROM bar CROSS JOIN bat
        |Has joins:
        |    true
        |
    """.trimMargin()
}
