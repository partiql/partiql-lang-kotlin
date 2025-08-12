package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.ast.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserChanges {
    @Test
    fun `Parser initialization`() {
        val parserDefault = PartiQLParser.default()
        val parserWithBuilder = PartiQLParser.builder().build()

        assertNotNull(parserDefault)
        assertNotNull(parserWithBuilder)
    }

    @Test
    fun `Parse a create table statement`() {
        val parser = PartiQLParser.default()
        val query = "CREATE TABLE myTable"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(query, result.source)
        assertTrue(result.root is Statement.DDL.CreateTable)
        assertNotNull(result.locations.get(result.root.tag))
    }

    @Test
    fun `Parse a insert into table statement`() {
        val parser = PartiQLParser.default()
        val query = "INSERT INTO tbl VALUES (1, 2, 3)"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(query, result.source)
        assertTrue(result.root is Statement.DML.Insert)
        assertNotNull(result.locations.get(result.root.tag))
    }

    @Test
    fun `Parse a select table statement`() {
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
            val ast = parser.parse("CREATE TABLE 'foo asd")
        }

        assertTrue { exception.message.contains("extraneous input '''") }
        assertNotNull(exception.location)
    }
}
