package org.partiql.transpiler

import org.partiql.ast.Statement
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.PartiQLPlan
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.spi.Plugin

/**
 * PartiQL query translation utilities.
 */
public class PartiQLTranspiler(
    private val plugins: List<Plugin>,
) {

    private val parser = PartiQLParserBuilder.standard().build()
    private val planner = PartiQLPlannerBuilder().plugins(plugins).build()

    @Throws(TranspilerException::class)
    public fun <T> transpile(source: String, target: TpTarget<T>, session: PartiQLPlanner.Session): Result<T> {
        try {
            val collector = ProblemCollector()
            // --->
            val ast = parse(source)
            val plan = plan(ast, session)
            // <----
            val output = target.retarget(plan, collector::callback)
            //
            return Result(source, ast, plan, output, collector.problems)
        } catch (e: TranspilerException) {
            throw e
        } catch (cause: Throwable) {
            throw TranspilerException("Transpiler exception", cause)
        }
    }

    private fun parse(source: String): Statement {
        val result = parser.parse(source)
        return result.root
    }

    private fun plan(statement: Statement, session: PartiQLPlanner.Session): PartiQLPlan {
        val result = planner.plan(statement, session)
        val errors = result.problems.filter { it.details.severity == ProblemSeverity.ERROR }
        if (errors.isNotEmpty()) {
            throw RuntimeException("Planner encountered errors: ${errors.joinToString()}")
        }
        return result.plan
    }

    public data class Result<T>(
        val source: String,
        val ast: Statement,
        val plan: PartiQLPlan,
        val output: TpOutput<T>,
        val problems: List<TranspilerProblem>,
    )

    private class ProblemCollector {

        internal val problems = mutableListOf<TranspilerProblem>()

        internal fun callback(problem: TranspilerProblem) {
            problems.add(problem)
        }
    }
}
