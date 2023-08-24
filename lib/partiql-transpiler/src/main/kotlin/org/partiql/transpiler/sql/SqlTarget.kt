package org.partiql.transpiler.sql

import org.partiql.ast.Statement
import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.PartiQLTranspilerTarget
import org.partiql.transpiler.ProblemCallback

/**
 * This is a base [PartiQLTranspilerTarget] for SQL dialects.
 */
public abstract class SqlTarget : PartiQLTranspilerTarget<String> {

    /**
     * Default SQL dialect for AST -> SQL.
     */
    open val dialect: SqlDialect = SqlDialect.PARTIQL

    /**
     * Default SQL formatting layout.
     */
    open val layout: SqlLayout = SqlLayout.DEFAULT

    /**
     * Default SQL call transformation logic.
     */
    open val calls: SqlCalls = SqlCalls.DEFAULT

    /**
     * Entry-point for manipulations of the [PartiQLPlan] tree.
     */
    abstract fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan

    /**
     * Apply the plan rewrite, then use the given [SqlDialect] to output SQL text.
     */
    override fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback): String {
        val newPlan = rewrite(plan, onProblem)
        val newAst = unplan(newPlan, onProblem)
        val block = dialect.apply(newAst)
        return block.sql(layout)
    }

    /**
     * Default Plan to AST translation. This method is only for potential edge cases
     */
    open fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): Statement {
        val transform = SqlTransform(plan.globals, calls, onProblem)
        val statement = transform.apply(plan.statement)
        return statement
    }
}
