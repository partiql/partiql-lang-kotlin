package org.partiql.eval.compiler

import org.partiql.eval.Environment
import org.partiql.eval.Expr
import org.partiql.eval.ExprRelation
import org.partiql.eval.Mode
import org.partiql.eval.Row
import org.partiql.eval.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.plan.rel.RelLimit
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * This class has an example of applying a custom operator strategy.
 */
public class StrategyTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.empty()

    private class MyLimit : ExprRelation {
        override fun open(env: Environment) = error("open")
        override fun close() = error("close")
        override fun hasNext(): Boolean = false
        override fun next(): Row = error("next")
    }

    @Test
    fun strategy() {
        var trigged = false
        val pattern = Pattern(RelLimit::class.java)
        val strategy = object : Strategy(pattern) {
            override fun apply(match: Match, callback: Callback): Expr {
                trigged = true
                return MyLimit()
            }
        }
        // ignore output
        val input = "select * from [1,2] limit 100"
        compile(input, strategy)
        // assert the strategy was triggered
        assertTrue(trigged, "the compiler did not apply the custom strategy")
    }

    // Helper to "compile with strategies".
    private fun compile(input: String, strategy: Strategy): Statement {
        val mode = Mode.STRICT()
        val ast = parser.parse(input).statements[0]
        val plan = planner.plan(ast, session).plan
        val compiler = PartiQLCompiler.builder()
            .addStrategy(strategy)
            .build()
        return compiler.prepare(plan, mode)
    }
}
