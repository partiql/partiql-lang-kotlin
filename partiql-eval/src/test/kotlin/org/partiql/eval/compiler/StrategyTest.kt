package org.partiql.eval.compiler

import org.partiql.eval.Expr
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.internal.operator.rex.ExprLit
import org.partiql.parser.PartiQLParser
import org.partiql.plan.rel.RelLimit
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * This class has an example of applying a custom operator strategy.
 */
public class StrategyTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.empty()
    private val nil = ExprLit(Datum.nullValue())

    @Test
    fun strategy() {
        var trigged = false
        val pattern = Pattern(RelLimit::class.java)
        val strategy = object : Strategy(pattern) {
            override fun apply(match: Match): Expr {
                trigged = true
                return nil
            }
        }
        // ignore output
        val input = "select * from [1,2] limit 100"
        compile(input, listOf(strategy))
        // assert the strategy was triggered
        assertTrue(trigged, "the compiler did not apply the custom strategy")
    }

    // Helper to "compile with strategies".
    private fun compile(input: String, strategies: List<Strategy>): Statement {
        val mode = Mode.STRICT()
        val ast = parser.parse(input).statements[0]
        val plan = planner.plan(ast, session).plan
        val compiler = PartiQLCompiler.builder()
            .addAllStrategies(strategies)
            .build()
        return compiler.prepare(plan, mode)
    }
}
