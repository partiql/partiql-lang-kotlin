package org.partiql.cli.pipeline

import org.partiql.ast.Statement
import org.partiql.cli.ErrorCodeString
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.io.PrintStream

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val compiler: PartiQLCompiler,
    private val ctx: Context,
    private val mode: Mode,
    private val debug: Boolean,
    private val out: PrintStream
) {

    /**
     * TODO replace with the ResultSet equivalent?
     * @throws PipelineException when there are accumulated errors, or if the components have thrown an [PRuntimeException].
     */
    @Throws(PipelineException::class)
    fun execute(statement: String, session: Session): Datum {
        val ast = parse(statement)
        val plan = plan(ast, session)
        return execute(plan, session)
    }

    /**
     * Parses, plans, and executes the statement, returning both the result datum and the planner-inferred type.
     */
    @Throws(PipelineException::class)
    fun executeWithType(statement: String, session: Session): Pair<Datum, PType> {
        val ast = parse(statement)
        val plan = plan(ast, session)
        val datum = execute(plan, session)
        val type = (plan.action as Action.Query).rex.type.pType
        return datum to type
    }

    private fun parse(source: String): Statement {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            parser.parse(source, ctx)
        }
        if (result.statements.size != 1) {
            throw PipelineException("Expected exactly one statement, got: ${result.statements.size}")
        }
        return result.statements[0]
    }

    private fun plan(statement: Statement, session: Session): Plan {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            planner.plan(statement, session, ctx)
        }
        val plan = result.plan
        if (debug) {
            PlanPrinter.print(plan, System.err)
        }
        return plan
    }

    private fun execute(plan: Plan, session: Session): Datum {
        val statement = listen(ctx.errorListener as AppPErrorListener) {
            compiler.prepare(plan, mode, ctx)
        }
        return listen(ctx.errorListener as AppPErrorListener) {
            statement.execute()
        }
    }

    private fun <T> listen(listener: AppPErrorListener, action: () -> T): T {
        listener.clear()
        val result = try {
            action.invoke()
        } catch (e: PipelineException) {
            throw e
        } catch (e: PRuntimeException) {
            val message = ErrorMessageFormatter.message(e.error)
            throw PipelineException(message)
        }
        if (listener.hasErrors()) {
            throw PipelineException("Failed with given input. Please see the above errors.")
        }
        return result
    }

    companion object {

        fun default(out: PrintStream, config: Config, debug: Boolean = false): Pipeline {
            return create(Mode.PERMISSIVE(), out, config, debug)
        }

        fun strict(out: PrintStream, config: Config, debug: Boolean = false): Pipeline {
            return create(Mode.STRICT(), out, config, debug)
        }

        private fun create(mode: Mode, out: PrintStream, config: Config, debug: Boolean = false): Pipeline {
            val listener = config.getErrorListener(out)
            val ctx = Context.of(listener)
            val parser = PartiQLParser.Builder().build()
            val planner = PartiQLPlanner.builder().build()
            val compiler = PartiQLCompiler.builder().build()
            return Pipeline(parser, planner, compiler, ctx, mode, debug, out)
        }
    }

    /**
     * Halts execution.
     */
    class PipelineException(override val message: String?) : PRuntimeException(
        PError(PError.INTERNAL_ERROR, Severity.ERROR(), PErrorKind.EXECUTION(), null, null)
    )

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
