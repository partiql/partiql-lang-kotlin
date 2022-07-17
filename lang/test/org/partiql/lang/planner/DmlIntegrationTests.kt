package org.partiql.lang.planner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.lang.ION
import org.partiql.lang.eval.ExprValueFactory

class DmlIntegrationTests {
    val valueFactory = ExprValueFactory.standard(ION)

    private val database = InMemoryDatabase(
        listOf(InMemoryTable("customer", listOf("id"), valueFactory))
    )

    // DL TOOD: include test for INSERT/SELECT

    @Test
    fun `insert, select and delete`() {
        val customerTbl = database.tables["customer"]!!
        assertEquals(0, customerTbl.size)

        // DL TODO: DML queries return { rows_effected: n } we should assert on that.

        // start by inserting 4 rows
        database.executeQuery("INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        database.executeQuery("INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")
        database.executeQuery("INSERT INTO customer << { 'id': 3, 'name': 'moe' } >>")
        database.executeQuery("INSERT INTO customer << { 'id': 4, 'name': 'sue' } >>")

        // assert each of the rows is present in the actual table.
        assertEquals(4, customerTbl.size)
        assertTrue(customerTbl.containsKey(intKey(1)))
        assertTrue(customerTbl.containsKey(intKey(2)))
        assertTrue(customerTbl.containsKey(intKey(3)))
        assertTrue(customerTbl.containsKey(intKey(4)))

        // run some simple SFW queries DL TODO: could DRY a little here.
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"bob\"}]"),
            database.executeQuery("SELECT c.name FROM customer AS c where c.id = 1").ionValue
        )
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"jane\"}]"),
            database.executeQuery("SELECT c.name FROM customer AS c where c.id = 2").ionValue
        )
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"moe\"}]"),
            database.executeQuery("SELECT c.name FROM customer AS c where c.id = 3").ionValue
        )
        // run some simple SFW queries
        assertEquals(
            ION.singleValue("\$partiql_bag::[{ name: \"sue\"}]"),
            database.executeQuery("SELECT c.name FROM customer AS c where c.id = 4").ionValue
        )

        // now delete 2 rows and assert that they are no longer present (test DELETE FROM with WHERE predicate)

        database.executeQuery("DELETE FROM customer AS c WHERE c.id = 2")
        assertEquals(3, customerTbl.size)
        assertFalse(customerTbl.containsKey(intKey(2)))

        database.executeQuery("DELETE FROM customer AS c WHERE c.id = 4")
        assertFalse(customerTbl.containsKey(intKey(4)))

        // finally, delete all remaining rows (test DELETE FROM without WHERE predicate)

        database.executeQuery("DELETE FROM customer")
        assertEquals(0, customerTbl.size)
        assertFalse(customerTbl.containsKey(intKey(1)))
        assertFalse(customerTbl.containsKey(intKey(3)))
    }

    private fun intKey(value: Int) = valueFactory.newList(listOf(valueFactory.newInt(value)))
}
