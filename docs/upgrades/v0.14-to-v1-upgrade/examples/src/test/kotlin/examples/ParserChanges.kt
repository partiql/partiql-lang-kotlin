package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.ast.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.SourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserChanges {
    @Test
    fun `PartiQLParser initialization`() {
        val parserDefault = PartiQLParser.default()

        // Builder returns the default for now and no additional options present
        val parserWithBuilder = PartiQLParser.builder().build()
    }

    @Test
    fun `Parse the 'CREATE TABLE' statement`() {
        val parser = PartiQLParser.default()
        val query = "CREATE TABLE myTable"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(query, result.source)
        assertTrue(result.root is Statement.DDL.CreateTable)
        assertNotNull(result.locations.get(result.root.tag))
    }

    @Test
    fun `Parse the 'INSERT INTO' statement`() {
        val parser = PartiQLParser.default()
        val query = "INSERT INTO tbl VALUES (1, 2, 3)"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(query, result.source)
        assertTrue(result.root is Statement.DML.Insert)
        assertNotNull(result.locations.get(result.root.tag))
    }

    @Test
    fun `Parse the 'SELECT FROM' statement`() {
        val parser = PartiQLParser.default()
        val query = "SELECT * FROM tbl WHERE id = 123"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(query, result.source)
        assertTrue(result.root is Statement.Query)
        assertNotNull(result.locations.get(result.root.tag))
    }

    @Test
    fun `Parser error handling`() {
        val parser = PartiQLParser.default()
        val exception = assertThrows<PartiQLParserException> {
            val ast = parser.parse("CREATE TABLE 'foo")
        }

        assertTrue { exception.message.contains("extraneous input '''") }
        assertEquals(SourceLocation(1, 14, 62, 1), exception.location)
    }
}
