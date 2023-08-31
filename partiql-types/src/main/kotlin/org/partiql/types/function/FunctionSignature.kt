package org.partiql.types.function

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Represents the signature of a PartiQL function.
 *
 * The signature includes the names of the function (which allows for function overloading),
 * the return type, a list of parameters, a flag indicating whether the function is deterministic
 * (i.e., always produces the same output given the same input), and an optional description.
 *
 * @property name               Function name
 * @property returns            Operator return type
 * @property parameters         Operator parameters
 * @property isDeterministic    Flag indicating this function always produces the same output given the same input.
 * @property isNullCall         Flag indicating if any of the call arguments is NULL, then return NULL.
 * @property isNullable         Flag indicating this function's operator may return a NULL value.
 * @property description        Optional operator description
 */
@OptIn(PartiQLValueExperimental::class)
public class FunctionSignature(
    public val name: String,
    public val returns: PartiQLValueType,
    public val parameters: List<FunctionParameter>,
    public val isDeterministic: Boolean = true,
    public val isNullCall: Boolean = false,
    public val isNullable: Boolean = true,
    public val description: String? = null,
) {

    /**
     * String mangling of a function signature to generate a specific identifier.
     *
     * Format NAME__INPUTS__RETURNS
     */
    private val specific = buildString {
        append(name.uppercase())
        append("__")
        append(parameters.joinToString("_") { it.type.name })
        append("__")
        append(returns.name)
    }

    /**
     * SQL-99 p.542 <deterministic characteristic>
     */
    private val deterministicCharacteristic = when (isDeterministic) {
        true -> "DETERMINISTIC"
        else -> "NOT DETERMINISTIC"
    }

    /**
     * SQL-99 p.543 <null-call clause>
     */
    private val nullCallClause = when (isNullCall) {
        true -> "RETURNS NULL ON NULL INPUT"
        else -> "CALLED ON NULL INPUT"
    }

    override fun toString(): String = buildString {
        val fn = name.uppercase()
        val indent = "  "
        append("CREATE FUNCTION \"$fn\" (")
        if (parameters.isNotEmpty()) {
            val extent = parameters.maxOf { it.name.length }
            for (i in parameters.indices) {
                val p = parameters[i]
                val ws = (extent - p.name.length) + 1
                appendLine()
                append(indent).append(p.name.uppercase()).append(" ".repeat(ws)).append(p.type.name)
                if (i != parameters.size - 1) append(",")
            }
        }
        appendLine(" )")
        append(indent).appendLine("RETURNS $returns")
        append(indent).appendLine("SPECIFIC $specific")
        append(indent).appendLine(deterministicCharacteristic)
        append(indent).appendLine(nullCallClause)
        append(indent).appendLine("RETURN $fn ( ${parameters.joinToString { it.name.uppercase() }} ) ;")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionSignature) return false
        if (
            other.name != name ||
            other.returns != returns ||
            other.isDeterministic != isDeterministic ||
            other.isNullCall != isNullCall ||
            other.isNullable != isNullable ||
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
