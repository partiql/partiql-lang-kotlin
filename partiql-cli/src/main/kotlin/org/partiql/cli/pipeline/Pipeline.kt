package org.partiql.cli.pipeline

import org.partiql.ast.Statement
import org.partiql.cli.ErrorCodeString
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.builder.PartiQLEngineBuilder
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PErrorListenerException
import java.io.PrintStream
import kotlin.jvm.Throws

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val engine: PartiQLEngine,
    private val ctx: Context,
    private val mode: PartiQLEngine.Mode
) {

    /**
     * TODO replace with the ResultSet equivalent?
     * @throws PipelineException when there are accumulated errors, or if the components have thrown an [PErrorListenerException].
     */
    @Throws(PipelineException::class)
    fun execute(statement: String, session: Session): PartiQLResult {
        val ast = parse(statement)
        val plan = plan(ast, session)
        return execute(plan, session)
    }

    private fun parse(source: String): Statement {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            parser.parse(source, ctx)
        }
        return result.root
    }

    private fun plan(statement: Statement, session: Session): Plan {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            planner.plan(statement, session, ctx)
        }
        return result.plan
    }

    private fun execute(plan: Plan, session: Session): PartiQLResult {
        val statement = listen(ctx.errorListener as AppPErrorListener) {
            engine.prepare(plan, session, ctx, mode)
        }
        return statement.execute(session)
    }

    private fun <T> listen(listener: AppPErrorListener, action: () -> T): T {
        listener.clear()
        val result = try {
            action.invoke()
        } catch (e: PipelineException) {
            throw e
        }
        if (listener.hasErrors()) {
            throw PipelineException("Failed with given input. Please see the above errors.")
        }
        return result
    }

    companion object {

        fun default(out: PrintStream, config: Config): Pipeline {
            return create(PartiQLEngine.Mode.PERMISSIVE, out, config)
        }

        fun strict(out: PrintStream, config: Config): Pipeline {
            return create(PartiQLEngine.Mode.STRICT, out, config)
        }

        private fun create(mode: PartiQLEngine.Mode, out: PrintStream, config: Config): Pipeline {
            val listener = config.getErrorListener(out)
            val ctx = Context.of(listener)
            val parser = PartiQLParserBuilder().build()
            val planner = PartiQLPlanner.builder().build()
            val engine = PartiQLEngineBuilder().build()
            return Pipeline(parser, planner, engine, ctx, mode)
        }
    }

    /**
     * Halts execution.
     */
    class PipelineException(override val message: String?) : PErrorListenerException(message)

    /**
     * Configuration for passing through user-defined configurations to the underlying components.
     */
    class Config(
        private val maxErrors: Int,
        private val inhibitWarnings: Boolean,
        private val warningsAsErrors: Array<ErrorCodeString>
    ) {
        fun getErrorListener(out: PrintStream): AppPErrorListener {
            return AppPErrorListener(out, maxErrors, inhibitWarnings, warningsAsErrors)
        }
    }
}
