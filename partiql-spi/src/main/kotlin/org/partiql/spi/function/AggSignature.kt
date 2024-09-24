package org.partiql.spi.function

import org.partiql.types.PType

/**
 * Represents the signature of a PartiQL aggregation function.
 *
 * @property isDecomposable     Flag indicating this aggregation can be decomposed
 * @constructor
 */
public class AggSignature(
    @JvmField public val name: String,
    @JvmField public val returns: PType,
    @JvmField public val parameters: List<Parameter>,
    @JvmField public val description: String? = null,
    @JvmField public val isNullable: Boolean = true,
    @JvmField public val isDecomposable: Boolean = true,
) {

    /**
     * Symbolic name of this operator of the form NAME__INPUTS__RETURNS
     */
    public val specific: String = buildString {
        append(name.uppercase())
        append("__")
        append(parameters.joinToString("_") { it.getType().kind.name })
        append("__")
        append(returns)
    }

    /**
     * Use the symbolic name for easy debugging
     *
     * @return
     */
    override fun toString(): String = specific

    override fun equals(other: Any?): Boolean {
        if (other !is AggSignature) return false
        if (
            other.name != name ||
            other.returns != returns ||
            other.parameters.size != parameters.size ||
            other.isDecomposable != isDecomposable ||
            other.isNullable != isNullable
        ) {
            return false
        }
        // all other parts equal, compare parameters (ignore names)
        for (i in parameters.indices) {
            val p1 = parameters[i]
            val p2 = other.parameters[i]
            if (p1.getType() != p2.getType()) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + returns.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + isDecomposable.hashCode()
        result = 31 * result + isNullable.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}
