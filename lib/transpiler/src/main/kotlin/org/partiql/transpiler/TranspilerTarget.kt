package org.partiql.transpiler

import org.partiql.ast.Statement
import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.block.BlockWriter

/**
 * A target determines the behavior of each stage of the transpilation.
 */
abstract class TranspilerTarget {
    abstract val target: String
    abstract val version: String
    abstract val dialect: Dialect

    abstract fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan

    public fun unparse(statement: Statement, onProblem: ProblemCallback): String {
        val root = dialect.write(statement, onProblem)
        return BlockWriter.write(root)
    }

    public fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): Statement {
        TODO()
    }
}
