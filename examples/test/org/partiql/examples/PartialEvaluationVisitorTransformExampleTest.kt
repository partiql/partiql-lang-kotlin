package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class PartialEvaluationVisitorTransformExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = PartialEvaluationVisitorTransformExample(out)

    override val expected = """
        |Original AST:
        |    (query (plus (lit 1) (lit 1)))
        |Transformed AST:
        |    (query (lit 2))
        |
    """.trimMargin()
}
