package org.partiql.eval.compiler

import org.junit.jupiter.api.Disabled
import org.partiql.eval.Expr
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.internal.operator.rex.ExprLit
import org.partiql.parser.V1PartiQLParser
import org.partiql.plan.rex.RexLit
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * This class has an example of applying a custom operator strategy.
 */
public class StrategyTest {

    private val parser = V1PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.empty()
    private val nil = ExprLit(Datum.nullValue())

    @Test
    fun strategy() {
        // the pattern matches a string literal that says "replace_me"
        val pattern = Pattern.match(RexLit::class.java).predicate {
            val rex = it as RexLit // matched, so mostly safe
            val datum = rex.getValue()
            if (datum.type.kind == PType.Kind.STRING) {
                datum.string == "replace_me"
            } else {
                false
            }
        }
            .build()
        // the strategy sets a flag to indicate it was applied, then stubs out the expression.
        var trigged = false
        val strategy = object : Strategy(pattern) {
            override fun apply(match: Match): Expr {
                trigged = true
                return nil
            }
        }
        // ignore output
        compile("'replace_me'", listOf(strategy))
        // assert the strategy was triggered
        assertTrue(trigged, "the compiler did not apply the custom strategy")
    }

    /**
     * Pattern:
     *
     *   RelFilter(A)
     *      \
     *      RelFilter(B)
     *
     * Strategy:
     *
     *    (RelFilter(A), RelFilter(B)) -> RelOpFilter(Expr(A) AND Expr(B))
     */
    @Test
    fun combineFilters() {
        // val pattern = Strategy.pattern(RelFilter::class.java)
        TODO()
    }

    @Test
    @Disabled("Modify and enable for debugging.")
    fun debug() {
        // placeholder
    }

    // Helper to "compile with strategies".
    private fun compile(input: String, strategies: List<Strategy>): Statement {
        val mode = Mode.STRICT()
        val ast = parser.parse(input).root
        val plan = planner.plan(ast, session).plan
        val compiler = PartiQLCompiler.builder().apply {
            for (s in strategies) {
                addStrategy(s)
            }
        }.build()
        return compiler.prepare(plan, mode)
    }
}
