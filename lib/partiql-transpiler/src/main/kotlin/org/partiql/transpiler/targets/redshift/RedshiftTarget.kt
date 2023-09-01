package org.partiql.transpiler.targets.redshift

import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.transpiler.sql.SqlTarget

/**
 * Experimental Redshift transpilation target.
 */
public object RedshiftTarget : SqlTarget() {

    override val target: String = "Redshift"

    override val version: String = "0"

    /**
     * Wire the Redshift call rewrite rules.
     */
    override val calls: SqlCalls = RedshiftCalls()

    /**
     * At this point, no plan rewriting.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = plan
}
