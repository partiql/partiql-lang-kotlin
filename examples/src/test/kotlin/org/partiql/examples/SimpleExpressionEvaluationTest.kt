package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class SimpleExpressionEvaluationTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = SimpleExpressionEvaluation(out)

    override val expected = """
        |PartiQL query:
        |    1 + 1
        |result
        |    2
        |
    """.trimMargin()
}
