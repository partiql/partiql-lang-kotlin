package org.partiql.eval.compiler

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session

/**
 * Strategy tests primarily test the matching algorithm, as the output operators are discarded.
 */
class StrategyTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.empty("default")

    // helper function to parse and plan the input expression
    private fun plan(input: String): Plan = planner.plan(parser.parse(input).root, session).plan

    @Test
    fun match() {
        val input = plan("1 + 1")
        println(input)
    }
}
