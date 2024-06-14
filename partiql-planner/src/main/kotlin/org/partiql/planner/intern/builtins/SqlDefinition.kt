package org.partiql.planner.intern.builtins

/**
 * Namespaced interfaces to make defining all the builtins easier.
 */
internal sealed interface SqlDefinition {

    interface Operator : SqlDefinition {
        fun getVariants(): Collection<org.partiql.planner.metadata.Operator>
    }

    interface Fn : SqlDefinition {
        fun getVariants(): Collection<org.partiql.planner.metadata.Fn>
    }
}
