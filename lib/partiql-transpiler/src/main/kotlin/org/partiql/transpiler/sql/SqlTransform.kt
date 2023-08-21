package org.partiql.transpiler.sql

import org.partiql.plan.Global
import org.partiql.transpiler.ProblemCallback
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Statement as PlanStatement

/**
 * [SqlTransform] represents extendable logic for translating from a [PlanNode] to [AstNode] tree.
 */
public open class SqlTransform(
    private val globals: List<Global>,
    private val onProblem: ProblemCallback,
) {

    public fun apply(statement: PlanStatement): AstStatement {
        TODO()
    }
}
