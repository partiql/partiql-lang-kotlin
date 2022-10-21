package org.partiql.lang.eval.builtins.windowFunctions

import org.junit.jupiter.api.Test
import org.partiql.lang.planner.TestContext

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
class LeadLagTest {

    @Test
    fun `lead_lag`() {
        val ctx = TestContext()

        // run some simple SFW queries
        ctx.executeAndAssert(
            "\$partiql_bag::[{previous_co:null},{previous_co:0.4},{previous_co:null},{previous_co:0.2}]",
            "SELECT lag(l.co) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as previous_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000} >> as l"
        )
        ctx.executeAndAssert(
            "\$partiql_bag::[{next_co:1.0},{next_co:null},{next_co:0.8},{next_co:null}]",
            "SELECT lead(l.co) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as next_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000} >> as l"
        )
        ctx.executeAndAssert(
            "\$partiql_bag::[{prev_of_prev_co:null},{prev_of_prev_co:null},{prev_of_prev_co:0.4},{prev_of_prev_co:null}, {prev_of_prev_co:null}, {prev_of_prev_co:0.2}]",
            "SELECT lag(l.co, 2) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as prev_of_prev_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000}, {'sensor' : 2, 'co': 1.3, 'recordtime': 5000}, {'sensor' : 1, 'co': 1.9, 'recordtime': 6000} >> as l"
        )
        ctx.executeAndAssert(
            "\$partiql_bag::[{next_of_next_co:1.9},{next_of_next_co:null},{next_of_next_co:null},{next_of_next_co:1.3},{next_of_next_co:null},{next_of_next_co:null}]",
            "SELECT lead(l.co,2) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as next_of_next_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000}, {'sensor' : 2, 'co': 1.3, 'recordtime': 5000}, {'sensor' : 1, 'co': 1.9, 'recordtime': 6000} >> as l"
        )
        ctx.executeAndAssert(
            "\$partiql_bag::[{previous_co:0.01},{previous_co:0.4},{previous_co:0.01},{previous_co:0.2}]",
            "SELECT lag(l.co,1,0.01) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as previous_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000} >> as l"
        )
        ctx.executeAndAssert(
            "\$partiql_bag::[{next_co:1.0},{next_co:0.01},{next_co:0.8},{next_co:0.01}]",
            "SELECT lead(l.co,1,0.01) OVER(PARTITION BY l.sensor ORDER BY l.recordtime) as next_co FROM << {'sensor' : 2, 'co': 0.2, 'recordtime': 1000}, {'sensor' : 1, 'co': 0.4, 'recordtime': 2000}, {'sensor' : 2, 'co': 0.8, 'recordtime': 3000}, {'sensor' : 1, 'co': 1.0, 'recordtime': 4000} >> as l"
        )
    }
}
