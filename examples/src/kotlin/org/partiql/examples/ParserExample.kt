package org.partiql.examples

import kotlin.test.*
import com.amazon.ion.system.*
import com.amazon.ionelement.api.toIonValue
import org.partiql.examples.util.Example
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.*
import java.io.PrintStream

/**
 * Demonstrates the use of [SqlParser] and [PartiqlAst].
 */
class ParserExample(out: PrintStream) : Example(out) {

    /** Demonstrates query parsing and SerDe.  */
    override fun run() {
        /// A standard instance of [IonSystem], which is required by [SqlParser].
        val ion = IonSystemBuilder.standard().build()

        // An instance of [SqlParser].
        val parser: Parser = SqlParser(ion)

        val query = "SELECT exampleField FROM exampleTable WHERE anotherField > 10"
        print("PartiQL query", query)

        // Use the SqlParser instance to parse the example query.  This is very simple.
        val originalAst = parser.parseExprNode(query)

        // Now the originalAst can be inspected, rewritten and/or serialized as needed.
        // For now we're just going to serialize and deserialize it.

        // Convert the ExprNode AST to the Ion s-expression form.
        val serializedAst = originalAst.toAstStatement()

        // Now we can convert the Ion s-expression form into an Ion value to pretty print
        val prettySerializedAst = serializedAst.toIonElement().asAnyElement().toIonValue(ion).toPrettyString()
        print("Serialized AST", prettySerializedAst)

        // Re-constitute the serialized AST.  The toExprNode will convert from any supported
        // version of the s-expression form to an instance of [ExprNode].
        val deserializedAst = serializedAst.toExprNode(ion)
        // Verify that we have the correct AST.
        assertEquals(originalAst, deserializedAst)

        // Here we show how to parse a query directly to a PartiqlAst statement
        val statement = parser.parseAstStatement(query)

        // We can easily convert the PartiqlAst statement into an Ion s-expression
        val elements = statement.toIonElement()
        // and back into a PartiqlAst statement
        val roundTrippedStatement = PartiqlAst.transform(elements) as PartiqlAst.Statement
        // Verify that we have the original Partiql Ast statement
        assertEquals(statement, roundTrippedStatement)
    }
}
