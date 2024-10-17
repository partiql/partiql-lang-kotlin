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
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PErrorListenerException
import java.io.PrintStream
import kotlin.jvm.Throws

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val engine: PartiQLEngine,
    private val plannerConfig: PlannerConfigImpl,
    private val compilerConfig: CompilerConfigImpl,
    private val parserConfig: ParserConfigImpl
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
        val result = listen(parserConfig.errorListener) {
            parser.parse(source, parserConfig)
        }
        return result.root
    }

    private fun plan(statement: Statement, session: Session): Plan {
        val result = listen(plannerConfig.errorListener) {
            planner.plan(statement, session, plannerConfig)
        }
        return result.plan
    }

    private fun execute(plan: Plan, session: Session): PartiQLResult {
        val statement = listen(compilerConfig.errorListener) {
            engine.prepare(plan, session, compilerConfig)
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
            val parser = PartiQLParserBuilder().build()
            val parserConfig = ParserConfigImpl(listener)
            val planner = PartiQLPlanner.builder().build()
            val plannerConfig = PlannerConfigImpl(listener)
            val compilerConfig = CompilerConfigImpl(mode, listener)
            val engine = PartiQLEngineBuilder().build()
            return Pipeline(parser, planner, engine, plannerConfig, compilerConfig, parserConfig)
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
