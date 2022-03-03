package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class ParserErrorExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = ParserErrorExample(out)

    override val expected = """
        |Invalid PartiQL query:
        |    SELECT 1 + 
        |Error message:
        |    Parser Error: at line 1, column 10: expected expression, OPERATOR : '+'
        |Error information:
        |    errorCode: PARSE_EXPECTED_EXPRESSION
        |    LINE_NUMBER: 1
        |    COLUMN_NUMBER: 10
        |    TOKEN_TYPE: OPERATOR
        |    TOKEN_VALUE: 
        |    '+'
        |
    """.trimMargin()
}
