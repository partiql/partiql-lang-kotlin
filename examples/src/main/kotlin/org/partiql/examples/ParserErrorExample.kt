package org.partiql.examples

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.examples.util.Example
import org.partiql.lang.errors.Property
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.io.PrintStream

/**
 * Demonstrates the use of [Parser], [AstSerializer] and [AstDeserializer].
 */
class ParserErrorExample(out: PrintStream) : Example(out) {

    /** A standard instance of [IonSystem], which is required by [Parser].  */
    internal var ion = IonSystemBuilder.standard().build()

    /** An instance of [Parser].  */
    private var parser: Parser = PartiQLParserBuilder().ionSystem(ion).build()

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
