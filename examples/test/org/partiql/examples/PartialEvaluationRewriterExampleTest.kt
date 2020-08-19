package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class PartialEvaluationRewriterExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = PartialEvaluationRewriterExample(out)

    override val expected = """
        |Original AST:
        |    (query (plus (lit 1) (lit 1)))
        |Rewritten AST:
        |    (query (lit 2))
        |
    """.trimMargin()
}
