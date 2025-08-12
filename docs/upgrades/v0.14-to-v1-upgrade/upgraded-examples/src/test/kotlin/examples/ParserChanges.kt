package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.ast.Query
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.dml.Insert
import org.partiql.parser.PartiQLParser
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PRuntimeException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserChanges {
    @Test
    fun `Parser initialization`() {
        val parserDefault = PartiQLParser.standard()
        val parserWithBuilder = PartiQLParser.builder().build()

        assertNotNull(parserDefault)
        assertNotNull(parserWithBuilder)
    }

    @Test
    fun `Parse a create table statement`() {
        val parser = PartiQLParser.standard()
        val query = "CREATE TABLE myTable"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertTrue(result.statements[0] is CreateTable)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse a insert into table statement`() {
        val parser = PartiQLParser.standard()
        val query = "INSERT INTO tbl VALUES (1, 2, 3)"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertTrue(result.statements[0] is Insert)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse a select table statement`() {
        val parser = PartiQLParser.standard()
        val query = "SELECT * FROM tbl WHERE id = 123"

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertTrue(result.statements[0] is Query)
        assertNotNull(result.locations.get(result.statements[0].tag))
    }

    @Test
    fun `Parse multiple statements`() {
        val parser = PartiQLParser.standard()
        val query = """
            CREATE TABLE myTable;
            INSERT INTO tbl VALUES (1, 2, 3);
            SELECT * FROM tbl WHERE id = 123;
        """.trimIndent()

        val result: PartiQLParser.Result = parser.parse(query)

        assertNotNull(result)
        assertEquals(3, result.statements.size)
        assertContentEquals(listOf("CreateTable", "Insert", "Query"), result.statements.map { s -> s.javaClass.simpleName })
    }

    @Test
    fun `Parser error handling`() {
        val parser = PartiQLParser.standard()
        val exception = assertThrows<PRuntimeException> {
            val ast = parser.parse("CREATE TABLE 'foo")
        }

        assertEquals(exception.error.code(), PError.UNEXPECTED_TOKEN)
    }
}
