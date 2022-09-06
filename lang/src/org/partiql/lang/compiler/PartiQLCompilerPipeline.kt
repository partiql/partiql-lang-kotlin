package org.partiql.lang.compiler

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.PartiQLStatement
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser

/**
 * [PartiQLCompilerPipeline] is the top-level class for embedded usage of PartiQL.
 *
 * Example usage:
 * ```
 * val pipeline = PartiQLCompilerPipeline.standard()
 * val session = // session bindings
 * val statement = pipeline.compile("-- some PartiQL query!")
 * val result = statement.eval(session)
 * when (result) {
 *   is PartiQLResult.Value -> handle(result)  // Query Result
 *   is PartiQLResult.Insert -> handle(result) // DML `Insert`
 *   is PartiQLResult.Delete -> handle(result) // DML `Delete`
 *   ...
 * }
 * ```
 */
class PartiQLCompilerPipeline private constructor(
    private val parser: Parser,
    private val planner: PartiQLPlanner,
    private val compiler: PartiQLCompiler
) {

    companion object {

        private val DEFAULT_ION = IonSystemBuilder.standard().build()

        /**
         *
         */
        @JvmStatic
        fun standard() = PartiQLCompilerPipeline(
            parser = SqlParser(DEFAULT_ION),
            planner = PartiQLPlannerBuilder.standard().build(),
            compiler = PartiQLCompilerBuilder.standard().build()
        )

        /**
         * Builder utility for easy pipeline creation.
         *
         * Example usage:
         * ```
         *
         * ```
         */
        fun build(block: Builder.() -> Unit): PartiQLCompilerPipeline {
            val builder = Builder()
            block.invoke(builder)
            return PartiQLCompilerPipeline(
                parser = builder.parser,
                planner = builder.planner.build(),
                compiler = builder.compiler.build(),
            )
        }
    }

    /**
     * Compiles a PartiQL query into an executable [PartiQLStatement].
     */
    fun compile(statement: String): PartiQLStatement {
        val ast = parser.parseAstStatement(statement)
        return compile(ast)
    }

    /**
     * Compiles a [PartiqlAst.Statement] representation of a query into an executable [PartiQLStatement].
     */
    fun compile(statement: PartiqlAst.Statement): PartiQLStatement {
        val result = planner.plan(statement)
        // TODO review error handling pattern with the PartiQL team
        if (result is PartiQLPlanner.Result.Error) {
            throw PartiQLException(result.problems.toString())
        }
        val plan = (result as PartiQLPlanner.Result.Success).plan
        return compile(plan)
    }

    /**
     * Compiles a [PartiqlPhysical.Plan] representation of a query into an executable [PartiQLStatement].
     */
    fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement {
        return compiler.compile(statement)
    }

    class Builder internal constructor() {
        // TODO replace with PartiQLParserBuilder after https://github.com/partiql/partiql-lang-kotlin/pull/711
        var parser = SqlParser(DEFAULT_ION)
        val planner = PartiQLPlannerBuilder.standard()
        val compiler = PartiQLCompilerBuilder.standard()
    }
}
