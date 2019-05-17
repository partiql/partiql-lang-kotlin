package org.partiql.examples

import org.junit.*
import kotlin.test.*
import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.syntax.*

/**
 * Demonstrates the use of [SqlParser], [AstSerializer] and [AstDeserializer].
 */
class ParseAndAstSerDe {

    /** A standard instance of [IonSystem], which is required by [SqlParser].  */
    internal var ion = IonSystemBuilder.standard().build()

    /** A simple example of PartiQL which we will be parsing below.  */
    private val EXAMPLE_QUERY = "SELECT exampleField FROM exampleTable WHERE anotherField > 10"

    /** The serialized form of [EXAMPLE_QUERY], the format of which is documented at `docs/dev/README-AST-V1.md`. */
    private val SERIALIZED_EXAMPLE_QUERY_AST = """
        (ast
          (version 1)
          (root
            (term
              (exp
                (select
                  (project
                    (list
                      (term
                        (exp
                          (id exampleField case_insensitive))
                        (meta (${'$'}source_location ({line_num:1, char_offset:8}))))))
                  (from
                    (term
                      (exp
                        (id exampleTable case_insensitive)) (meta (${'$'}source_location ({line_num:1, char_offset:26})))))
                  (where
                    (term
                      (exp
                        (>
                          (term
                            (exp (id anotherField case_insensitive))
                            (meta (${'$'}source_location ({line_num:1, char_offset:45}))))
                          (term
                            (exp
                              (lit 10)) (meta (${'$'}source_location ({line_num:1, char_offset:60}))))))
                      (meta (${'$'}source_location ({line_num:1, char_offset:58}))))))))))
    """

    /** The contents of [SERIALIZED_EXAMPLE_QUERY_AST], loaded into an [IonSexp] with [IonSystem.singleValue].  */
    private val EXPECTED_AST = ion.singleValue(SERIALIZED_EXAMPLE_QUERY_AST) as IonSexp

    /** An instance of [SqlParser].  */
    private var parser: Parser = SqlParser(ion)

    /** An AST deserializer.  This instance has no support for deserializing custom meta information.  */
    private var deserializer = AstDeserializerBuilder(ion).build()

    /** Demonstrates query parsing and SerDe.  */
    @Test
    fun parseAndSerDe() {
        // Use the SqlParser instance to parse the example query.  This is very simple.
        val originalAst = parser.parseExprNode(EXAMPLE_QUERY)

        // Now the originalAst can be inspected, rewritten and/or serialized as needed.
        // For now we're just going to serialize and deserialize it.

        // Convert the ExprNode AST to the Ion s-expression form.
        // AstSerializer always serializes to the latest version of the s-expression format.
        val actualAst = AstSerializer.serialize(originalAst, ion)

        // Verify that the AST has been serialized correctly.
        assertEquals(actualAst, EXPECTED_AST)

        // Re-constitute the serialized AST.  The deserializer will convert from any supported
        // version of the s-expression form to an instance of [ExprNode].
        val deserializedAst = deserializer.deserialize(actualAst)

        // Verify that we have the correct AST.
        assertEquals(originalAst, deserializedAst)
    }

    /** Demonstrates handling of syntax errors.  */
    @Test
    fun parseSyntaxError() {
        try {
            // Attempt to parse a query with invalid syntax.
            parser.parseExprNode("SELECT 1 + ")
            fail("ParserException was not thrown")
        }
        catch (e: ParserException) {
            // ParserException inherits from SqlException and so contains various properties
            // containing information pertaining to the error.
            assertEquals(ErrorCode.PARSE_EXPECTED_EXPRESSION, e.errorCode)
            val errorContext = e.errorContext
            assertNotNull(errorContext)
            assertEquals(1, errorContext[Property.LINE_NUMBER]!!.longValue())
            assertEquals(10, errorContext[Property.COLUMN_NUMBER]!!.longValue())
        }
    }
}
