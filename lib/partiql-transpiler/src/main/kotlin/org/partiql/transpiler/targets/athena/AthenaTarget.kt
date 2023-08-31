package org.partiql.transpiler.targets.athena

import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.transpiler.sql.SqlTarget

/**
 * Experimental Athena transpilation target.
 */
public object AthenaTarget : SqlTarget() {

    override val target: String = "Athena"

    override val version: String = "3"

    /**
     * Wire the Athena call rewrite rules.
     */
    override val calls: SqlCalls = AthenaCalls()

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = plan
}
