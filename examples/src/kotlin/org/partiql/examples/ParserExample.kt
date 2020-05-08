package org.partiql.examples

import kotlin.test.*
import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.ast.*
import org.partiql.lang.syntax.*
import java.io.PrintStream

/**
 * Demonstrates the use of [SqlParser], [AstSerializer] and [AstDeserializer].
 */
class ParserExample(out: PrintStream) : Example(out) {

    /** Demonstrates query parsing and SerDe.  */
    override fun run() {
        /// A standard instance of [IonSystem], which is required by [SqlParser].
        val ion = IonSystemBuilder.standard().build()

        // An instance of [SqlParser].
        val parser: Parser = SqlParser(ion)

        // An AST deserializer.  This instance has no support for deserializing custom meta information.
        val deserializer = AstDeserializerBuilder(ion).build()

        val query = "SELECT exampleField FROM exampleTable WHERE anotherField > 10"
        print("PartiQL query", query)

        // Use the SqlParser instance to parse the example query.  This is very simple.
        val originalAst = parser.parseExprNode(query)

        // Now the originalAst can be inspected, rewritten and/or serialized as needed.
        // For now we're just going to serialize and deserialize it.

        // Convert the ExprNode AST to the Ion s-expression form.
        // AstSerializer always serializes to the latest version of the s-expression format.
        // the serialized format is documented at `docs/dev/README-AST-V0.md`
        val serializedAst = AstSerializer.serialize(originalAst, AstVersion.V2, ion)
        print("Serialized AST", serializedAst.toPrettyString())

        // Re-constitute the serialized AST.  The deserializer will convert from any supported
        // version of the s-expression form to an instance of [ExprNode].
        val deserializedAst = deserializer.deserialize(serializedAst, AstVersion.V2)

        // Verify that we have the correct AST.
        assertEquals(originalAst, deserializedAst)
    }
}
