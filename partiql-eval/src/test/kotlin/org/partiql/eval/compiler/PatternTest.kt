package org.partiql.eval.compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Operation
import org.partiql.plan.Operator
import org.partiql.plan.rex.RexCall
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session

public class PatternTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.empty()

    @Test
    fun acceptance() {
        // placeholder
    }

    @Test
    fun negative() {
        // placeholder
    }

    @Test
    // @Disabled("Modify and enable for debugging.")
    fun debug() {
        val op = parse("1 + 1")
        val pattern = Strategy.pattern(RexCall::class.java)
        assertTrue(pattern.matches(op))
    }

    private fun parse(query: String): Operator {
        val ast = parser.parse(query).root
        val plan = planner.plan(ast, session).plan
        val operation = plan.getOperation()
        if (operation is Operation.Query) {
            return operation.getRex()
        }
        throw IllegalStateException("Expected a query operation, got: $operation")
    }
}
