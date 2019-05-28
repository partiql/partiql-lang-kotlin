package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class PartialEvaluationRewriterExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = PartialEvaluationRewriterExample(out)

    override val expected = """
        |Original AST:
        |    (ast (version 1) (root (term (exp (+ (term (exp (lit 1)) (meta (${'$'}source_location ({line_num:1,char_offset:1})))) (term (exp (lit 1)) (meta (${'$'}source_location ({line_num:1,char_offset:5})))))) (meta (${'$'}source_location ({line_num:1,char_offset:3}))))))
        |Rewritten AST:
        |    (ast (version 1) (root (term (exp (lit 2)) (meta (${'$'}source_location ({line_num:1,char_offset:3}))))))
        |
    """.trimMargin()
}