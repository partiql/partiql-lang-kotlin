package org.partiql.planner.intern.validate

import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

/**
 * SqlNamespace is responsible for name resolution.
 *
 * Previously Rel.Binding held data, and TypeEnv held contain logic.
 */
internal data class SqlBinding(
    @JvmField val name: String,
    @JvmField val type: StaticType,
    @JvmField val ordinal: Int,
) {

    /**
     * Returns true iff the binding name matches the id.
     */
    fun matches(id: Identifier.Symbol): Boolean = id.matches(name)

    /**
     * Searches for the [Identifier.Symbol] within this binding's [StaticType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     */
    fun contains(id: Identifier.Symbol): Boolean? = type.contains(id)

    /**
     * TODO REMOVE ME ONCE SqlPlanner REPLACES PlanTyper (aka split up PlanTyper).
     *
     * Return an IR node which seems not-so relevant anymore.
     */
    fun binding(): Rel.Binding = Rel.Binding(name, type)

    /**
     * Return an IR variable reference to this binding.
     */
    fun reference(): Rex = Rex(type, Rex.Op.Var.Local(0, ordinal))

    /**
     * Searches for the [Identifier.Symbol] within the given [StaticType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     */
    private fun StaticType.contains(id: Identifier.Symbol): Boolean? {
        return when (val type = this.flatten()) {
            is StructType -> type.contains(id)
            is AnyOfType -> {
                val anyKnownToContainKey = type.allTypes.any { it.contains(id) == true }
                val anyKnownToNotContainKey = type.allTypes.any { it.contains(id) == false }
                val anyNotKnownToContainKey = type.allTypes.any { it.contains(id) == null }
                when {
                    anyKnownToNotContainKey.not() && anyNotKnownToContainKey.not() -> true
                    anyKnownToContainKey.not() && anyNotKnownToContainKey -> false
                    else -> null
                }
            }
            is AnyType -> null
            else -> false
        }
    }

    /**
     * Searches for the [Identifier.Symbol] within the given [StructType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     */
    private fun StructType.contains(id: Identifier.Symbol): Boolean? {
        for (f in fields) {
            if (id.matches(f.key)) {
                return true
            }
        }
        val closed = constraints.contains(TupleConstraint.Open(false))
        return if (closed) false else null
    }

    /**
     * Compares [Identifier.Symbol] to [target] String using.
     */
    private fun Identifier.Symbol.matches(target: String): Boolean = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> target == symbol
        Identifier.CaseSensitivity.INSENSITIVE -> target.equals(symbol, ignoreCase = true)
    }
}
