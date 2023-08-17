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
}
