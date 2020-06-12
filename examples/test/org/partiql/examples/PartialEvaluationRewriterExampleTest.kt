package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class PartialEvaluationRewriterExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = PartialEvaluationRewriterExample(out)

    override val expected = """
        |Original AST:
        |    (meta (plus (meta (lit 1) {line:1,column:1}) (meta (lit 1) {line:1,column:5})) {line:1,column:3})
        |Rewritten AST:
        |    (meta (lit 2) {line:1,column:3})
        |
    """.trimMargin()
}
