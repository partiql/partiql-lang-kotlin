package org.partiql.cli.pipeline

import org.partiql.planner.PlannerConfig

class PlannerConfigImpl(
    private val listener: AppErrorListener
) : PlannerConfig {
    override fun getErrorListener(): AppErrorListener = listener
}
