package org.partiql.transpiler.sql

import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.TpOutput
import org.partiql.transpiler.TpTarget
import org.partiql.types.StaticType
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Statement as PlanStatement

public class SqlOutput(schema: StaticType, value: String) : TpOutput<String>(schema, value) {

    override fun toString(): String = value

    override fun toDebugString(): String = buildString {
        append("SQL: ").appendLine(value)
        append("Schema: ").appendLine(schema)
    }
}

/**
 * This is a base [TpTarget] for SQL dialects.
 */
public abstract class SqlTarget : TpTarget<String> {

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
    override fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback): TpOutput<String> {
        val newPlan = rewrite(plan, onProblem)
        if (newPlan.statement !is PlanStatement.Query) {
            error("Transpilation is currently only supported for query statements")
        }
        val schema = (newPlan.statement as PlanStatement.Query).root.type
        val newAst = unplan(newPlan, onProblem)
        val block = dialect.apply(newAst)
        val sql = block.sql(layout)
        return SqlOutput(schema, sql)
    }

    /**
     * Default Plan to AST translation. This method is only for potential edge cases
     */
    open fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): AstStatement {
        val transform = SqlTransform(plan.globals, calls, onProblem)
        val statement = transform.apply(plan.statement)
        return statement
    }
}
