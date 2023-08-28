package org.partiql.transpiler.targets

import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.sql.SqlTarget

/**
 * Athena target placeholder.
 */
public object AthenaTarget : SqlTarget() {

    override val target: String = "Athena"

    override val version: String = "3"

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = plan
}