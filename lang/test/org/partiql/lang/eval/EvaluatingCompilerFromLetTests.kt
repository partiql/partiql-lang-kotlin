package org.partiql.lang.eval

import org.junit.Ignore
import org.junit.Test

class EvaluatingCompilerFromLetTests : EvaluatorTestBase() {

    private val session = mapOf("A" to "[ { id : 1 } ]",
        "B" to "[ { id : 1 } ]",
        "C" to "[ { id : 1 } ]").toSession()


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

    //@Ignore
    @Test
    fun `LET variable available in WHERE clause`() =
        assertEval(
            "SELECT 1 FROM A LET 1 AS X WHERE X = 1",
            "[{ _1: 1 }]",
            session
        )
    @Test
    fun asdfsd() {
        println(eval("SELECT x FROM [1] LET 1 AS X WHERE X = 1"))
    }


    @Ignore
    @Test
    fun `LET variable available in GROUP BY clause`() =
        assertEval(
            "",
            "<< { '_1': 1 } >>",
            session
        )

    @Ignore
    @Test
    fun `LET variable available in HAVING clause`() =
        assertEval(
            "",
            "<< { '_1': 1 } >>",
            session
        )

    @Ignore
    @Test
    fun `LET variable available in SELECT clause`() =
        assertEval(
            "SELECT X FROM A LET 1 AS X",
            "<< { '_1': 1 } >>",
            session
        )

    @Ignore
    @Test
    fun `LET variable available in LIMIT clause`() =
        assertEval(
            "SELECT 1 FROM A LET 1 AS X LIMIT X",
            "<< { '_1': 1 } >>",
            session
        )

    // Error test cases:
}