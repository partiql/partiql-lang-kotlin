package org.partiql.cli.pipeline

import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemSeverity
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParser
import org.partiql.plan.v1.PartiQLPlan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val engine: PartiQLEngine,
) {

    /**
     * TODO replace with the ResultSet equivalent?
     */
    fun execute(statement: String, session: Session): PartiQLResult {
        val ast = parse(statement)
        val plan = plan(ast, session)
        return execute(plan, session)
    }

    private fun parse(source: String): Statement {
        val result = parser.parse(source)
        return result.root
    }

    private fun plan(statement: Statement, session: Session): PartiQLPlan {
        val callback = ProblemListener()
        val result = planner.plan(statement, session, callback)
        val errors = callback.problems.filter { it.details.severity == ProblemSeverity.ERROR }
        if (errors.isNotEmpty()) {
            throw RuntimeException(errors.joinToString())
        }
        TODO("Add V1 planner to the CLI")
    }

    private fun execute(plan: PartiQLPlan, session: Session): PartiQLResult {
        // val statement = engine.prepare(plan, session.mode, session.planner())
        // return engine.execute(statement)
        TODO("Add V1 planner to the CLI")
    }

    private class ProblemListener : ProblemCallback {

        val problems = mutableListOf<Problem>()

        override fun invoke(p1: Problem) {
            problems.add(p1)
        }
    }

    companion object {

        fun default(): Pipeline {
            val parser = PartiQLParser.default()
            val planner = PartiQLPlanner.standard()
            val engine = PartiQLEngine.standard()
            return Pipeline(parser, planner, engine)
        }

        fun strict(): Pipeline {
            val parser = PartiQLParser.default()
            val planner = PartiQLPlanner.builder().signal().build()
            val engine = PartiQLEngine.standard()
            return Pipeline(parser, planner, engine)
        }
    }
}
