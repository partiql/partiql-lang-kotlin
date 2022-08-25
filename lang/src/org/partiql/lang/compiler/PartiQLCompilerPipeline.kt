package org.partiql.lang.compiler

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.PartiQLStatement
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser

class PartiQLCompilerPipeline(
    private val parser: Parser,
    private val planner: PartiQLPlanner,
    private val compiler: PartiQLCompiler
) {

    companion object {

        private val ION_STANDARD = IonSystemBuilder.standard().build()

        @Deprecated(
            message = "To be removed in 1.0 release",
            replaceWith = ReplaceWith("standard"),
            level = DeprecationLevel.WARNING
        )
        @JvmStatic
        fun legacy(ion: IonSystem = ION_STANDARD): CompilerPipeline.Builder = CompilerPipeline.builder(ion)

        @JvmStatic
        fun standard(ion: IonSystem = ION_STANDARD) = PartiQLCompilerPipeline(
            parser = SqlParser(ion),
            planner = PartiQLPlannerBuilder.standard(ion).build(),
            compiler = PartiQLCompilerBuilder.standard(ion).build()
        )
    }

    fun compile(statement: String): PartiQLStatement {
        val ast = parser.parseAstStatement(statement)
        return compile(ast)
    }

    fun compile(statement: PartiqlAst.Statement): PartiQLStatement {
        val result = planner.plan(statement)
        // error handling is likely to be refactored after design review with the PartiQL team
        if (result is PartiQLPlanner.Result.Error) {
            throw PartiQLException(result.problems.toString())
        }
        val plan = (result as PartiQLPlanner.Result.Success).plan
        return compile(plan)
    }

    fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement {
        return compiler.compile(statement)
    }
}
