package org.partiql.transpiler.targets.trino

import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.transpiler.sql.SqlTarget

/**
 * Experimental Athena transpilation target.
 */
public object TrinoTarget : SqlTarget() {

    override val target: String = "Trino"

    override val version: String = "3"

    /**
     * Wire the Athena call rewrite rules.
     */
    override val calls: SqlCalls = TrinoCalls()

    /**
     * At this point, no plan rewriting.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = plan
}
