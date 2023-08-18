package org.partiql.types.function

import org.partiql.types.PartiQLValueType

/**
 * Represents the signature of a PartiQL function.
 *
 * The signature includes the names of the function (which allows for function overloading),
 * the return type, a list of parameters, a flag indicating whether the function is deterministic
 * (i.e., always produces the same output given the same input), and an optional description.
 */
public class FunctionSignature(
    public val name: String,
    public val returns: PartiQLValueType,
    public val parameters: List<FunctionParameter> = emptyList(),
    public val isDeterministic: Boolean = true,
    public val description: String? = null,
) {

    override fun toString(): String = buildString {
        val fn = name.uppercase()
        val indent = "  "
        val extent = parameters.maxOf { it.name.length }
        append("CREATE FUNCTION \"$fn\" (")
        for (p in parameters) {
            val ws = (extent - p.name.length) + 1
            val type = when (p) {
                is FunctionParameter.T -> "TYPE (${p.type})"
                is FunctionParameter.V -> p.type.name
            }
            appendLine()
            append(indent).append(p.name.uppercase()).append(" ".repeat(ws)).append(type)
        }
        appendLine(" )")
        append(indent).appendLine("RETURNS $returns")
        append(indent).appendLine("SPECIFIC -")
        append(indent).appendLine("RETURN $fn ( ${parameters.joinToString { it.name.uppercase() }} ) ;")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionSignature) return false
        if (
            other.name != name ||
            other.returns != returns ||
            other.isDeterministic != isDeterministic ||
            other.parameters.size != parameters.size
        ) {
            return false
        }
        // all other parts equal, compare parameters (ignore names)
        for (i in parameters.indices) {
            val p1 = parameters[i]
            val p2 = other.parameters[i]
            if (p1.type != p2.type) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + returns.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + isDeterministic.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}
