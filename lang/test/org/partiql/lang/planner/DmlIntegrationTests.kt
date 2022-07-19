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
        listOf(
            InMemoryTable("customer", listOf("id"), valueFactory),
            InMemoryTable("more_customer", listOf("id"), valueFactory)
        )
    )
    // DL TOOD: include test for INSERT/SELECT

    private fun executeAndAssert(
        expectedResultAsIonText: String,
        sql: String,
    ) {
        val expectedIon = ION.singleValue(expectedResultAsIonText)
        val result = database.executeQuery(sql)
        assertEquals(expectedIon, result.ionValue)
    }

    @Test
    fun `insert, select and delete`() {
        val customerTbl = database.tables["customer"]!!
        assertEquals(0, customerTbl.size)

        // start by inserting 4 rows
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 3, 'name': 'moe' } >>")
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 4, 'name': 'sue' } >>")

        // assert each of the rows is present in the actual table.
        assertEquals(4, customerTbl.size)
        assertTrue(customerTbl.containsKey(intKey(1)))
        assertTrue(customerTbl.containsKey(intKey(2)))
        assertTrue(customerTbl.containsKey(intKey(3)))
        assertTrue(customerTbl.containsKey(intKey(4)))

        // run some simple SFW queries
        executeAndAssert("\$partiql_bag::[{ name: \"bob\"}]", "SELECT c.name FROM customer AS c where c.id = 1")
        executeAndAssert("\$partiql_bag::[{ name: \"jane\"}]", "SELECT c.name FROM customer AS c where c.id = 2")
        executeAndAssert("\$partiql_bag::[{ name: \"moe\"}]", "SELECT c.name FROM customer AS c where c.id = 3")
        executeAndAssert("\$partiql_bag::[{ name: \"sue\"}]", "SELECT c.name FROM customer AS c where c.id = 4")

        // now delete 2 rows and assert that they are no longer present (test DELETE FROM with WHERE predicate)

        executeAndAssert("{rows_effected:1}", "DELETE FROM customer AS c WHERE c.id = 2")
        assertEquals(3, customerTbl.size)
        assertFalse(customerTbl.containsKey(intKey(2)))

        executeAndAssert("{rows_effected:1}", "DELETE FROM customer AS c WHERE c.id = 4")
        assertFalse(customerTbl.containsKey(intKey(4)))

        // finally, delete all remaining rows (test DELETE FROM without WHERE predicate)

        executeAndAssert("{rows_effected:2}", "DELETE FROM customer")
        assertEquals(0, customerTbl.size)
        assertFalse(customerTbl.containsKey(intKey(1)))
        assertFalse(customerTbl.containsKey(intKey(3)))
    }

    @Test
    fun `insert with select`() {
        val customerTbl = database.tables["customer"]!!
        customerTbl.truncate()
        assertEquals(0, customerTbl.size)
        // first put some data into the customer table
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 1, 'name': 'bob' } >>")
        executeAndAssert("{rows_effected:1}", "INSERT INTO customer << { 'id': 2, 'name': 'jane' } >>")

        // copy that data into the more customer_table by INSERTing the result of an SFW query
        executeAndAssert("{rows_effected:2}", "INSERT INTO more_customer SELECT c.id, c.name FROM customer AS c")

        val moreCustomerTbl = database.tables["more_customer"]!!
        assertEquals(2, customerTbl.size)
        assertTrue(moreCustomerTbl.containsKey(intKey(1)))
        assertTrue(moreCustomerTbl.containsKey(intKey(2)))

        // lastly, assert we have the correct data
        executeAndAssert("\$partiql_bag::[{ name: \"bob\"}]", "SELECT c.name FROM more_customer AS c where c.id = 1")
        executeAndAssert("\$partiql_bag::[{ name: \"jane\"}]", "SELECT c.name FROM more_customer AS c where c.id = 2")
    }

    private fun intKey(value: Int) = valueFactory.newList(listOf(valueFactory.newInt(value)))
}
