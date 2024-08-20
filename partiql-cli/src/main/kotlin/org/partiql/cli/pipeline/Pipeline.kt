package org.partiql.cli.pipeline

import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemSeverity
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.connector.Connector
import java.time.Instant
import org.partiql.planner.catalog.Session as PlannerSession

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val engine: PartiQLEngine,
) {

    /**
     * Combined planner and engine session.
     */
    internal data class Session(
        @JvmField val queryId: String,
        @JvmField val userId: String,
        @JvmField val currentCatalog: String,
        @JvmField val currentDirectory: List<String>,
        @JvmField val connectors: Map<String, Connector>,
        @JvmField val instant: Instant,
        @JvmField val debug: Boolean,
        @JvmField val mode: PartiQLEngine.Mode,
    ) {

        private val catalogs = connectors.values.map { it.getCatalog() }

        fun planner() = PlannerSession.builder()
            .identity(userId)
            .namespace(currentDirectory)
            .catalog(currentCatalog)
            .catalogs(*catalogs.toTypedArray())
            .build()

        fun engine() = PartiQLEngine.Session(
            catalogs = connectors,
            mode = mode
        )
    }

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
        val result = planner.plan(statement, session.planner(), callback)
        val errors = callback.problems.filter { it.details.severity == ProblemSeverity.ERROR }
        if (errors.isNotEmpty()) {
            throw RuntimeException(errors.joinToString())
        }
        return result.plan
    }

    private fun execute(plan: PartiQLPlan, session: Session): PartiQLResult {
        val statement = engine.prepare(plan, session.engine())
        return engine.execute(statement)
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
