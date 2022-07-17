package org.partiql.lang.planner.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.ExprValueFactory

class TestContext {
    val db = InMemoryDatabase().also {
        it.createTable("customer", listOf("id"))
    }
    val queryEngine = QueryEngine(db)
}

class IntegrationTests {
    val valueFactory = ExprValueFactory.standard(ION)

    @Test
    fun `select by key`() {
        val tc = TestContext()
        val queryEngine = tc.queryEngine

        println(queryEngine.executeQuery("INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>"))
        println(queryEngine.executeQuery("INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>"))

        queryEngine.enableDebugOutput = true
        println(queryEngine.executeQuery("SELECT * FROM customer AS c WHERE c.id = 2"))
        println(queryEngine.executeQuery("SELECT * FROM customer AS c WHERE c.id = 2 AND c.name = 'herman'"))
    }

    @Test
    fun `insert, select and delete`() {
        val tc = TestContext()
        val db = tc.db
        val queryEngine = tc.queryEngine

        val customerTableId = db.findTableMetadata(BindingName("customer", BindingCase.SENSITIVE))!!.tableId
        assertEquals(0, db.tableRowCount(customerTableId))

        // DL TODO: DML queries return { rows_effected: n } we should assert on that.

        // start by inserting 4 rows
        queryEngine.executeQuery("INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        queryEngine.executeQuery("INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")
        queryEngine.executeQuery("INSERT INTO customer << { 'id': 3, 'name': 'moe' } >>")
        queryEngine.executeQuery("INSERT INTO customer << { 'id': 4, 'name': 'sue' } >> ")

        queryEngine.enableDebugOutput = true
        // assert each of the rows is present in the actual table.
        assertEquals(4, db.tableRowCount(customerTableId))
        assertTrue(db.tableContainsKey(customerTableId, intKey(1)))
        assertTrue(db.tableContainsKey(customerTableId, intKey(2)))
        assertTrue(db.tableContainsKey(customerTableId, intKey(3)))
        assertTrue(db.tableContainsKey(customerTableId, intKey(4)))

        // run some simple SFW queries DL TODO: could DRY a little here.
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"bob\"}]"),
            queryEngine.executeQuery("SELECT c.name FROM customer AS c where c.id = 1").ionValue
        )
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"jane\"}]"),
            queryEngine.executeQuery("SELECT c.name FROM customer AS c where c.id = 2").ionValue
        )
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"moe\"}]"),
            queryEngine.executeQuery("SELECT c.name FROM customer AS c where c.id = 3").ionValue
        )
        // run some simple SFW queries
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"sue\"}]"),
            queryEngine.executeQuery("SELECT c.name FROM customer AS c where c.id = 4").ionValue
        )

        // now delete 2 rows and assert that they are no longer present (test DELETE FROM with WHERE predicate)

        queryEngine.executeQuery("DELETE FROM customer AS c WHERE c.id = 2")
        assertEquals(3, db.tableRowCount(customerTableId))
        assertFalse(db.tableContainsKey(customerTableId, intKey(2)))

        queryEngine.executeQuery("DELETE FROM customer AS c WHERE c.id = 4")
        assertFalse(db.tableContainsKey(customerTableId, intKey(4)))

        // finally, delete all remaining rows (test DELETE FROM without WHERE predicate)

        queryEngine.executeQuery("DELETE FROM customer")
        assertEquals(0, db.tableRowCount(customerTableId))
        assertFalse(db.tableContainsKey(customerTableId, intKey(1)))
        assertFalse(db.tableContainsKey(customerTableId, intKey(3)))
    }

    private fun intKey(value: Int) = valueFactory.newList(listOf(valueFactory.newInt(value)))
}
