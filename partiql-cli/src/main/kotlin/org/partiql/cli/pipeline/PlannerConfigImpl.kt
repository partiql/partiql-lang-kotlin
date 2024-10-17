package org.partiql.cli.pipeline

import org.partiql.planner.PlannerConfig

class PlannerConfigImpl(
    private val listener: AppPErrorListener
) : PlannerConfig {
    override fun getErrorListener(): AppPErrorListener = listener
}
