package org.partiql.eval.internal.plan

import org.partiql.eval.Mode

internal data class ExecutionPlanImpl(val root: PExpr, val mode: Mode)
