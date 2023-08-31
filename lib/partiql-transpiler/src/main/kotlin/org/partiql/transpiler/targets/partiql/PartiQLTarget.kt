package org.partiql.transpiler.targets.partiql

import org.partiql.plan.PartiQLPlan
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.sql.SqlTarget

/**
 * Default PartiQL Target does nothing as there is no need to rewrite the plan.
 */
public object PartiQLTarget : SqlTarget() {

    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = plan
}