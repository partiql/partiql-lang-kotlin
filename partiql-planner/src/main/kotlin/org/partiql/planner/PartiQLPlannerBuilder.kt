package org.partiql.planner

import org.partiql.spi.Plugin
import org.partiql.types.TypingMode

/**
 * PartiQLPlannerBuilder
 */
class PartiQLPlannerBuilder {

    private var plugins: List<Plugin> = emptyList()
    private var passes: List<PartiQLPlannerPass> = emptyList()
    private var mode: TypingMode = TypingMode.STRICT

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(plugins, passes, mode)

    public fun plugins(plugins: List<Plugin>): PartiQLPlannerBuilder = this.apply {
        this.plugins = plugins
    }

    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes = passes
    }

    public fun mode(mode: TypingMode): PartiQLPlannerBuilder = this.apply { this.mode = mode }
}
