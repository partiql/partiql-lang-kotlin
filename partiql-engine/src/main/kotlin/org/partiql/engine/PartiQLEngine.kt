package org.partiql.engine

import org.partiql.plan.PartiQLPlan
import org.partiql.spi.Plugin
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

public interface PartiQLEngine {
    public fun execute(plan: PartiQLPlan): Result

    public sealed interface Result {
        public data class Success @OptIn(PartiQLValueExperimental::class) constructor(
            val output: PartiQLValue
        )

        public data class Error @OptIn(PartiQLValueExperimental::class) constructor(
            val output: PartiQLValue
        )
    }

    public class Builder {
        private var plugins: List<Plugin> = emptyList()
        public fun withPlugins(plugins: List<Plugin>): Builder = this.apply {
            this.plugins = plugins
        }
        public fun build(): PartiQLEngine = TODO()
    }
}
