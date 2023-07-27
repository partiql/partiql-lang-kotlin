@file:OptIn(ExperimentalPartiQLSchemaInferencer::class)

package org.partiql.transpiler

import org.partiql.annotations.ExperimentalPartiQLSchemaInferencer
import org.partiql.ast.Statement
import org.partiql.ast.helpers.toLegacyAst
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.planner.transforms.AstToPlan
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencer
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.PartiQLPlan

/**
 * PartiQL query translation utilities.
 */
public class Transpiler(
    private val target: TranspilerTarget,
    private val context: PartiQLSchemaInferencer.Context,
) {

    private val parser = PartiQLParserBuilder.standard().build()

    @Throws(TranspilerException::class)
    public fun transpile(source: String): Result {
        try {
            val collector = ProblemCollector()
            // --->
            val inStatement = parse(source)
            val inPlan = plan(inStatement)
            // <----
            val outPlan = target.retarget(inPlan, collector::callback)
            val outStatement = target.unplan(outPlan, collector::callback)
            val outSource = target.unparse(outStatement, collector::callback)
            //
            return Result(target, source, outStatement, outPlan, outSource, collector.problems)
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

    // temporary
    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun plan(statement: Statement): PartiQLPlan {
        val ast = statement.toLegacyAst() as PartiqlAst.Statement
        val untyped = AstToPlan.transform(ast)
        val plan = PartiQLSchemaInferencer.type(untyped, context)
        return plan
    }

    public data class Result(
        val target: TranspilerTarget,
        val source: String,
        val ast: Statement,
        val plan: PartiQLPlan,
        val sql: String,
        val problems: List<TranspilerProblem>,
    )

    private class ProblemCollector {

        internal val problems = mutableListOf<TranspilerProblem>()

        internal fun callback(problem: TranspilerProblem) {
            problems.add(problem)
        }
    }
}
