package org.partiql.lang.eval

import junitparams.Parameters
import org.junit.Ignore
import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.to

class EvaluatingCompilerFromLetTests : EvaluatorTestBase() {

    private val session = mapOf("A" to "[ { id : 1 } ]",
        "B" to "[ { 'id' : 1 }, { 'id' : 2 } ]",
        "C" to "[ { id : 1 }, { 'id' : 2 }, { 'id' : 3 } ]",
        "customers" to """[
            { customerId: 123, firstName: "John", lastName: "Smith", age: 23},
            { customerId: 456, firstName: "Rob", lastName: "Jones", age: 45},
            { customerId: 789, firstName: "Emma", lastName: "Miller", age: 67}
        ]""",
        "orders" to """[
            { customerId: 123, sellerId: 1, productId: 11111, cost: 1 },
            { customerId: 123, sellerId: 2, productId: 22222, cost: 2 },
            { customerId: 123, sellerId: 1, productId: 33333, cost: 3 },
            { customerId: 456, sellerId: 2, productId: 44444, cost: 4 },
            { customerId: 456, sellerId: 1, productId: 55555, cost: 5 },
            { customerId: 456, sellerId: 2, productId: 66666, cost: 6 },
            { customerId: 789, sellerId: 1, productId: 77777, cost: 7 },
            { customerId: 789, sellerId: 2, productId: 88888, cost: 8 },
            { customerId: 789, sellerId: 1, productId: 99999, cost: 9 },
            { customerId: 100, sellerId: 2, productId: 10000, cost: 10 }
        ]""").toSession()

    /**
     *  Valid test cases:
     *    - LET defined variable available in:
     *        - WHERE
     *        - GROUP BY
     *        - HAVING
     *        - SELECT
     *        - LIMIT
     *        - Subquery?
     *    - Using an alias or table name defined in FROM within a LET clause
     *    - LET defined within subquery has different scoping and can be redefined
     *    - Chained LET variables (i.e. 1 AS X, X + 1 AS Y...)
     *    - Redefined LET variables from within LET clause (i.e. 1 AS X, 2 AS X)
     *    - Calling functions from LET clause
     *    - Defining a LET as a subquery (i.e. LET (SFW query) AS foo)
     *  Error-throwing cases:
     *    - Binding set by FROM clause set again? (... FROM foo AS f LET 1 AS f)
     *    - LET binding from inner query not available to outer query (throws exception)
     *    - Unbound variable from LET clause (i.e. if foo is unbound, LET foo AS f)
     *
     */

    // Valid test cases:

    @Test
    @Parameters
    fun letSingleClauseTests(tc: EvaluatorTestCase) = runTestCase(tc, session)

    fun parametersForLetSingleClauseTests() =
        listOf(
            // WHERE clause
            EvaluatorTestCase(
                query = "SELECT * FROM A LET 1 AS X WHERE X = 1",
                expectedSql = """<< { 'id': 1 } >>"""),
            // GROUP BY
            EvaluatorTestCase(
                query = "SELECT * FROM orders LET customerId AS X GROUP BY X",
                expectedSql = """<< { 'X': 100 }, { 'X': 123 }, { 'X': 456 }, { 'X': 789 } >>"""),
            // HAVING
            // SELECT
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X",
                expectedSql = """<< { 'X': 1 } >>"""),
            // LIMIT
            EvaluatorTestCase(
                query = "SELECT * FROM C LET 2 AS X LIMIT X",
                expectedSql = """<< { 'id': 1 }, { 'id': 2 } >>""")
        )

    @Test
    fun `LET shadowed bindings` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X, 2 AS X",
                expectedSql = """<< { 'X': 2 } >>"""),
                session = session)
    }

    @Test
    fun `LET clause using other variables` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X, Y FROM A LET 1 AS X, X + 1 AS Y",
                expectedSql = """<< { 'X': 1, 'Y': 2 } >>"""),
                session = session)
    }

    @Test
    fun `LET clause recursive binding` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X, X AS X",
                expectedSql = """<< { 'X': 1 } >>"""),
                session = session)
    }

    @Test
    fun `LET clause calling function` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET upper('foo') AS X",
                expectedSql = """<< { 'X': 'FOO' } >>"""),
                session = session)
    }

    // Error test cases:
    @Test
    fun `LET clause unbound variable` () {
        checkInputThrowingEvaluationException(
            "SELECT X FROM A LET Y AS X",
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 21L,
                    Property.BINDING_NAME to "Y"
            )
        )
    }
}