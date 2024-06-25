package org.partiql.planner.internal.fn

import org.partiql.planner.metadata.Routine

/**
 * This is a temporary solution as a standin to the system.
 */
internal class FnBuiltins private constructor(
    private val scalars: Map<String,List<Routine.Scalar>>,
    private val aggregations: Map<String,List<Routine.Aggregation>>,
) {

    fun getFunctions(name: String): List<Routine.Scalar> = emptyList()

    fun getAggregations(name: String): List<Routine.Aggregation> = emptyList()

    companion object {

        @JvmStatic
        val DEFAULT = load()

        @JvmStatic
        private fun load(): FnBuiltins {
            return FnBuiltins(
                scalars = emptyMap(),
                aggregations = emptyMap(),
            )
        }
    }
}
