package org.partiql.lang.planner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.planner.memorydb.MemoryDatabase
import org.partiql.lang.planner.memorydb.QueryEngine

class TestContext {
    val db = MemoryDatabase().also {
        it.createTable("customer", listOf("id"))
        it.createTable("more_customer", listOf("id"))
    }
    val queryEngine = QueryEngine(db)

    // Executes [sql]
    fun executeAndAssert(
        expectedResultAsIonText: String,
        sql: String,
    ) {
        val expectedIon = ION.singleValue(expectedResultAsIonText)
        val result = queryEngine.executeQuery(sql)
        assertEquals(expectedIon, result.toIonValue(ION))
    }

    fun intKey(value: Int) = db.valueFactory.newList(listOf(db.valueFactory.newInt(value)))
}

/**
 * Tests the query planner with some basic DML and SFW queries against using [QueryEngine] and [MemoryDatabase].
 */
class IntegrationTests {

    @Test
    fun `insert, select and delete`() {
        val ctx = TestContext()
        val db = ctx.db
        val customerMetadata = db.findTableMetadata(BindingName("customer", BindingCase.SENSITIVE))!!

        // start by inserting 4 rows
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 3, 'name': 'moe' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 4, 'name': 'sue' } >>")

        // assert each of the rows is present in the actual table.
        assertEquals(4, db.getRowCount(customerMetadata.tableId))
        assertTrue(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(1)))
        assertTrue(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(2)))
        assertTrue(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(3)))
        assertTrue(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(4)))

        // commented code intentionally kept.  Uncomment to see detailed debug information in the console when
        // this test is run
        // ctx.queryEngine.enableDebugOutput = true

        // run some simple SFW queries
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"bob\"}]", "SELECT c.name FROM customer AS c WHERE c.id = 1")
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"jane\"}]", "SELECT c.name FROM customer AS c WHERE c.id = 2")
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"moe\"}]", "SELECT c.name FROM customer AS c WHERE c.id = 3")
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"sue\"}]", "SELECT c.name FROM customer AS c WHERE c.id = 4")

        // now delete 2 rows and assert that they are no longer present (test DELETE FROM with WHERE predicate)

        ctx.executeAndAssert("{rows_effected:1}", "DELETE FROM customer AS c WHERE c.id = 2")
        assertEquals(3, db.getRowCount(customerMetadata.tableId))
        assertFalse(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(2)))

        ctx.executeAndAssert("{rows_effected:1}", "DELETE FROM customer AS c WHERE c.id = 4")
        assertFalse(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(4)))

        // finally, delete all remaining rows (test DELETE FROM without WHERE predicate)

        ctx.executeAndAssert("{rows_effected:2}", "DELETE FROM customer")
        assertEquals(0, db.getRowCount(customerMetadata.tableId))
        assertFalse(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(1)))
        assertFalse(db.tableContainsKey(customerMetadata.tableId, ctx.intKey(3)))
    }

    @Test
    fun `insert with select`() {
        val ctx = TestContext()
        val db = ctx.db
        // first put some data into the customer table
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 3, 'name': 'moe' } >>")
        ctx.executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 4, 'name': 'sue' } >>")

        // copy that data into the more_customer table by INSERTing the result of an SFW query
        ctx.executeAndAssert(
            "{rows_effected:2}",
            "INSERT INTO more_customer SELECT c.id, c.name FROM customer AS c WHERE c.id IN (1, 3)"
        )

        val moreCustomerMetadata = db.findTableMetadata(BindingName("more_customer", BindingCase.SENSITIVE))!!
        assertEquals(2, db.getRowCount(moreCustomerMetadata.tableId))
        assertTrue(db.tableContainsKey(moreCustomerMetadata.tableId, ctx.intKey(1)))
        assertTrue(db.tableContainsKey(moreCustomerMetadata.tableId, ctx.intKey(3)))

        // lastly, assert we have the correct data
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"bob\"}]", "SELECT c.name FROM more_customer AS c where c.id = 1")
        ctx.executeAndAssert("\$partiql_bag::[{ name: \"moe\"}]", "SELECT c.name FROM more_customer AS c where c.id = 3")
    }
}
