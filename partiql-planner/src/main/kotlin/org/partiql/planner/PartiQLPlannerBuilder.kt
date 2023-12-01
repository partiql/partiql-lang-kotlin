package org.partiql.planner

import org.partiql.spi.Plugin

/**
 * PartiQLPlannerBuilder
 */
class PartiQLPlannerBuilder {

    private var headers: MutableList<Header> = mutableListOf(PartiQLHeader)
    private var plugins: List<Plugin> = emptyList()
    private var passes: List<PartiQLPlannerPass> = emptyList()

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(headers, plugins, passes)

    public fun plugins(plugins: List<Plugin>): PartiQLPlannerBuilder = this.apply {
        this.plugins = plugins
    }

    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes = passes
    }

    public fun headers(headers: List<Header>): PartiQLPlannerBuilder = this.apply {
        this.headers += headers
    }
}
