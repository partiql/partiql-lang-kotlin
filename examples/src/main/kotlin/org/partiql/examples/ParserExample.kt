package org.partiql.examples

import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.examples.util.Example
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.io.PrintStream

/**
 * Demonstrates the use of [Parser] and [PartiqlAst].
 */
class ParserExample(out: PrintStream) : Example(out) {

    /** Demonstrates query parsing and SerDe.  */
    override fun run() {
        // An instance of [Parser].
        val parser: Parser = PartiQLParserBuilder().build()

        // A query in string format
        val query = "SELECT exampleField FROM exampleTable WHERE anotherField > 10"
        print("PartiQL query", query)

        // Use the SqlParser instance to parse the example query and get the AST as a PartiqlAst Statement.
        val ast = parser.parseAstStatement(query)

        // We can transform the AST into SexpElement form to pretty print
        val elements = ast.toIonElement()

        // Create an IonWriter to print the AST
        val astString = StringBuilder()
        val ionWriter = IonTextWriterBuilder.minimal().withPrettyPrinting().build(astString)

        // Now use the IonWriter to write the SexpElement AST into the StringBuilder and pretty print it
        elements.writeTo(ionWriter)
        print("Serialized AST", astString.toString())

        // We can also convert the SexpElement AST back into a PartiqlAst statement
        val roundTrippedStatement = PartiqlAst.transform(elements) as PartiqlAst.Statement
        // Verify that we have the original Partiql Ast statement
        if (ast != roundTrippedStatement) {
            throw Exception("Expected statements to be the same")
        }
    }
}
