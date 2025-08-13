package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.ast.Query
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.dml.Insert
import org.partiql.parser.PartiQLParser
import org.partiql.spi.Context
import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserChanges {
    @Test
    fun `PartiQLParser initialization`() {
        // Renamed from default() to standard()
        val parserDefault = PartiQLParser.standard()

        // Builder returns the standard for now and no additional options present
        val parserWithBuilder = PartiQLParser.builder().build()
    }

    @Test
    fun `Parse the 'CREATE TABLE' statement`() {
        val parser = PartiQLParser.standard()
        val query = "CREATE TABLE myTable"

        val result: PartiQLParser.Result = parser.parse(query)

        // The Source property of the result object is removed.
        // A list of ASTs is returned in the result object to support multiple statement parsing
        // and AST classes are refactoring to different classes
        // Tag is an integer type instead of String
        assertNotNull(result)
        assertTrue(result.statements[0] is CreateTable)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse the 'INSERT INTO' statement`() {
        val parser = PartiQLParser.standard()
        val query = "INSERT INTO tbl VALUES (1, 2, 3)"

        val result: PartiQLParser.Result = parser.parse(query)

        // The Source property of the result object is removed.
        // A list of ASTs is returned in the result object to support multiple statement parsing
        // and AST classes are refactoring to different classes
        // Tag is an integer type instead of String
        assertNotNull(result)
        assertTrue(result.statements[0] is Insert)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse the 'SELECT FROM' statement`() {
        val parser = PartiQLParser.standard()
        val query = "SELECT * FROM tbl WHERE id = 123"

        val result: PartiQLParser.Result = parser.parse(query)

        // The Source property of the result object is removed.
        // A list of ASTs is returned in the result object to support multiple statement parsing
        // and AST classes are refactoring to different classes
        // Tag is an integer type instead of String
        assertNotNull(result)
        assertTrue(result.statements[0] is Query)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse multiple statements`() {

        // Parsing multiple statements in one call is not supported before v1
        // A list of statement is returned in the result object after parsing ,u
        val parser = PartiQLParser.standard()
        val query = """
            CREATE TABLE myTable;
            INSERT INTO tbl VALUES (1, 2, 3);
            SELECT * FROM tbl WHERE id = 123;
        """.trimIndent()

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(3, result.statements.size)
        assertTrue(result.statements[0] is CreateTable)
        assertTrue(result.statements[1] is Insert)
        assertTrue(result.statements[2] is Query)
    }

    @Test
    fun `Parser error handling`() {
        val parser = PartiQLParser.standard()

        val exception = assertThrows<PRuntimeException> {
            // an invalid single quote is added before Table name foo
            parser.parse("CREATE TABLE 'foo")
        }

        assertEquals(exception.error.code(), PError.UNEXPECTED_TOKEN)
        assertEquals(PErrorKind.SYNTAX, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
        assertEquals(SourceLocation(1, 14, 1), exception.error.location)
    }

    @Test
    fun `Parser with custom error handler`() {
        class CustomPErrorListener : PErrorListener {
            val errorCollection: MutableList<PError> = mutableListOf()

            override fun report(error: PError) {
                when (error.severity.code()) {
                    // You may consider a customizing error message and throw the error, or you can record the error and then throw when it reaches maximum count
                    // In this case, we record only without throwing
                    Severity.ERROR -> errorCollection.add(error)
                    // You may consider a customizing error message, treat warning as an error and then throw, or you can suppress all warnings.
                    // In this case, we record only without throwing
                    Severity.WARNING -> errorCollection.add(error)
                    else -> error("This shouldn't have occurred.")
                }
            }
        }

        // Register Error Listener
        val listener = CustomPErrorListener()
        val context: Context = Context.of(listener)

        val parser = PartiQLParser.standard()
        val parseResult: PartiQLParser.Result
        try {
            // an invalid single quote is added before Table name foo
            parseResult = parser.parse("CREATE TABLE 'foo", context)
        } catch (ex: PRuntimeException) {
            // Since we register custom error handler to record all errors instead of throwing, these exceptions were likely unexpected.
            // However, the PartiQL library may throw PRuntimeExceptions as well. You should be able to unwrap these exceptions and provide quality
            // error messages to your users. In this case, we throw
            throw ex
        }

        // process your listener
        val exception = listener.errorCollection[0]

        assertEquals(PError.UNEXPECTED_TOKEN, exception.code())
        assertEquals(PErrorKind.SYNTAX, exception.kind.code())
        assertEquals(Severity.ERROR, exception.severity.code())
        assertEquals(SourceLocation(1, 14, 1), exception.location)
    }
}
