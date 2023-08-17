package org.partiql.planner.typer

import org.partiql.types.PartiQLValueType

/**
 * Going with a matrix here (using enum ordinals) as it's simple.
 */
private typealias TypeGraph = Array<Array<Relationship?>>

/**
 * Each edge represents a type relationship
 */
private class Relationship(val castSafety: CastSafety)

/**
 * A CAST is safe iff it's lossless and never errs.
 */
private enum class CastSafety { SAFE, UNSAFE }

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * Is this indeed a lattice? It's a rather smart sounding word.
 */
internal class TypeLattice private constructor(
    private val graph: TypeGraph,
) {

    companion object {

        public fun partiql(): TypeLattice {
            val n = PartiQLValueType.values().size
            val graph = arrayOfNulls<Array<Relationship?>>(n)
            // graph.set(PartiQLValueType.NULLABLE_INT8, canSafelyCastAs())
            // TODO!
            return TypeLattice(graph.requireNoNulls())
        }

        // TODO!
        private fun canSafelyCastAs(): Array<PartiQLValueType> {
            return emptyArray<PartiQLValueType>()
        }

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)
    }

    /**
     * TODO
     *
     * @param t1
     * @param t2
     * @return
     */
    public fun canSafelyCast(t1: PartiQLValueType, t2: PartiQLValueType): Boolean {
        val relationship = graph[t1][t2]
        return when (relationship) {
            null -> false
            else -> relationship.castSafety == CastSafety.SAFE
        }
    }

    /**
     * Dump the graph as DOT
     */
    override fun toString(): String {
        return super.toString()
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)
    //
    // private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = set(t.ordinal, value)
    //
    // private operator fun <T> Array<T?>.get(t: PartiQLValueType): T? = get(t.ordinal)


}
