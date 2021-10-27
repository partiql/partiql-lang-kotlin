package org.partiql.examples

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.syntax.*
import java.io.PrintStream

/**
 * Demonstrates the use of [SqlParser], [AstSerializer] and [AstDeserializer].
 */
class ParserErrorExample(out: PrintStream) : Example(out) {

    /** A standard instance of [IonSystem], which is required by [SqlParser].  */
    internal var ion = IonSystemBuilder.standard().build()

    /** An instance of [SqlParser].  */
    private var parser: Parser = SqlParser(ion)

    /** Demonstrates handling of syntax errors.  */
    override fun run() = try {
        // Attempt to parse a query with invalid syntax.
        val invalidQuery = "SELECT 1 + "
        print("Invalid PartiQL query:", invalidQuery)
        parser.parseExprNode(invalidQuery)

        throw Exception("ParserException was not thrown")
    } catch (e: ParserException) {
        val errorContext = e.errorContext!!

        val errorInformation = "errorCode: ${e.errorCode}" +
                "\nLINE_NUMBER: ${errorContext[Property.LINE_NUMBER]}" +
                "\nCOLUMN_NUMBER: ${errorContext[Property.COLUMN_NUMBER]}" +
                "\nTOKEN_TYPE: ${errorContext[Property.TOKEN_TYPE]}" +
                "\nTOKEN_VALUE: ${errorContext[Property.TOKEN_VALUE]}"
        print("Error message:", e.generateMessage())
        print("Error information:", errorInformation)
    }
}
