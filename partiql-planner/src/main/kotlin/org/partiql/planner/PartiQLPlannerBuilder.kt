package org.partiql.planner

import org.partiql.spi.Plugin

/**
 * PartiQLPlannerBuilder
 */
class PartiQLPlannerBuilder {

    private var plugins: List<Plugin> = emptyList()
    private var passes: List<PartiQLPlannerPass> = emptyList()

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(plugins, passes)

    public fun plugins(plugins: List<Plugin>): PartiQLPlannerBuilder = this.apply {
        this.plugins = plugins
    }

    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes = passes
    }
}
