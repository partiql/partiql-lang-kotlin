package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.SourceLocation
import org.partiql.planner.PartiQLPlanner
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorHandlingChanges {
    /**
     * The example shows when using {@link PartiQLParser} to parse string statement,
     * the {@link PartiQLParserException} will be thrown. The parser will stop and throw the exception
     *  when an error occurs.
     */
    @Test
    fun `Parser error handling`() {
        val parser = PartiQLParser.default()

        val exception = assertThrows<PartiQLParserException> {
            try {
                val parseResult = parser.parse("Create Table 'mytable")
            } catch (e: PartiQLParserException) {
                // rethrow for exception validation in test
                throw e
            }
        }

        assertTrue { exception.message.contains("extraneous input") }
        assertEquals("UNRECOGNIZED", exception.tokenType)
        assertEquals(SourceLocation(1, 14, 62, 1), exception.location)
    }

    /**
     * The example shows when using {@link PartiQLPlanner} to make plan from AST.
     * There is no specific exception defined in the planner, so the planner will throw any exception
     *  when an error occurs.
     */
    @Test
    fun `PLanner evaluation error`() {
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val session = PartiQLPlanner.Session(
            queryId = Random().nextInt().toString(),
            userId = "test-user",
            currentCatalog = "default",
            catalogs = mapOf()
        )

        val exception = assertThrows<Exception> {
            try {
                val parseResult = parser.parse("Create Table mytable")
                val planResult = planner.plan(parseResult.root, session)
            } catch (e: Exception) {
                // PartiQLParserException will be thrown during PartiQL process the query in the major PartiQL components
                throw e
            }
        }

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message!!.contains("Unsupported statement"))
    }
}
