package org.partiql.transpiler.targets

import org.partiql.ast.Ast
import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.plan.Case
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Rex
import org.partiql.transpiler.Dialect
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.TranspilerTarget
import org.partiql.transpiler.dialects.PartiQLDialect
import org.partiql.types.StaticType

object RedshiftTarget : TranspilerTarget() {

    override val target: String = "Redshift"

    override val version: String = "0"

    override val dialect: Dialect = PartiQLDialect.INSTANCE

    override fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): Statement {
        val ruleset = RedshiftRuleset(onProblem)
        val expr = ruleset.visitRex(plan.root, Unit)
        return Ast.statementQuery(expr)
    }

    private class RedshiftRuleset(onProblem: ProblemCallback) : BaseRuleset(onProblem) {

        override fun visitRexQueryCollection(node: Rex.Query.Collection, ctx: Unit) = unplan {
            val sfw = node.rel.toSFW(this@RedshiftRuleset)
            // ERR: Missing projection!
            if (node.constructor == null && sfw.select == null) {
                val problem = TranspilerProblem(
                    level = TranspilerProblem.Level.ERROR,
                    message = "Unsupported query plan; the input plan is missing a SELECT",
                )
                onProblem(problem)
            }
            // ERR: Double projection!
            if (node.constructor != null && sfw.select != null) {
                val problem = TranspilerProblem(
                    level = TranspilerProblem.Level.ERROR,
                    message = "Unsupported query plan; the input plan is malformed",
                )
                onProblem(problem)
            }
            if (node.constructor != null) {
                // SELECT VALUE
                val constructor = visitRex(node.constructor!!, Unit)
                val setq: SetQuantifier? = null // not yet supported
                if (node.constructor!!.grabType() != StaticType.STRUCT || constructor !is Expr.Struct) {
                    onProblem(
                        TranspilerProblem(
                            level = TranspilerProblem.Level.ERROR,
                            message = "SELECT VALUE constructor must be of type STRUCT for Redshift SELECT"
                        ))
                    sfw.select = selectProject(emptyList(), setq)
                } else {
                    onProblem(
                        TranspilerProblem(
                            level = TranspilerProblem.Level.WARNING,
                            message = "SELECT VALUE has been rewritten to Redshift `SELECT object()`"
                        ))
                    sfw.select = selectProject(listOf(redshiftObjectCall(constructor)), setq)
                }
            }
            sfw.build()
        }

        private fun redshiftObjectCall(struct: Expr.Struct): Select.Project.Item = unplan {
            val args = mutableListOf<Expr>()
            struct.fields.forEach {
                args.add(it.name)
                args.add(it.value)
            }
            val call = exprCall(
                function = id("object", case = Case.INSENSITIVE),
                args = args
            )
            selectProjectItemExpression(call, null)
        }
    }
}
