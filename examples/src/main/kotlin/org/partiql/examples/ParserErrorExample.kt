package org.partiql.examples

import org.partiql.errors.Property
import org.partiql.examples.util.Example
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.io.PrintStream

/**
 * Demonstrates the use of [Parser].
 */
class ParserErrorExample(out: PrintStream) : Example(out) {

    /** An instance of [Parser].  */
    private var parser: Parser = PartiQLParserBuilder().build()

    /** Demonstrates handling of syntax errors.  */
    override fun run() = try {
        // Attempt to parse a query with invalid syntax.
        val invalidQuery = "SELECT 1 + "
        print("Invalid PartiQL query:", invalidQuery)
        parser.parseAstStatement(invalidQuery)

        throw Exception("ParserException was not thrown")
    } catch (e: ParserException) {
        val errorContext = e.errorContext

        val errorInformation = "errorCode: ${e.errorCode}" +
            "\nLINE_NUMBER: ${errorContext[Property.LINE_NUMBER]}" +
            "\nCOLUMN_NUMBER: ${errorContext[Property.COLUMN_NUMBER]}" +
            "\nTOKEN_DESCRIPTION: ${errorContext[Property.TOKEN_DESCRIPTION]}" +
            "\nTOKEN_VALUE: ${errorContext[Property.TOKEN_VALUE]}"
        print("Error message:", e.generateMessage())
        print("Error information:", errorInformation)
    }
}
