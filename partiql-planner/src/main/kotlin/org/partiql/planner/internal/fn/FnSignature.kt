package org.partiql.planner.internal.fn

import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 *
 * The signature includes the names of the function (which allows for function overloading),
 * the return type, a list of parameters, a flag indicating whether the function is deterministic
 * (i.e., always produces the same output given the same input), and an optional description.
 *
 * @property name               Function name
 * @property returns            Operator return type
 * @property parameters         Operator parameters
 * @property description        Optional operator description
 * @property isDeterministic    Flag indicating this function always produces the same output given the same input.
 * @property isNullable         Flag indicating this function's operator may return a NULL value.
 * @property isNullCall         Flag indicating if any of the call arguments is NULL, then return NULL.
 * @property isMissable         Flag indicating this function's operator may return a MISSING value.
 * @property isMissingCall      Flag indicating if any of the call arguments is MISSING, the return MISSING.
 */
@FnExperimental
@OptIn(PartiQLValueExperimental::class)
public data class FnSignature(
    @JvmField public val name: String,
    @JvmField public val returns: PType,
    @JvmField public val parameters: List<FnParameter>,
    @JvmField public val description: String? = null,
    @JvmField public val isDeterministic: Boolean = true,
    @JvmField public val isNullable: Boolean = true,
    @JvmField public val isNullCall: Boolean = false,
    @JvmField public val isMissable: Boolean = true,
    @JvmField public val isMissingCall: Boolean = true,
) {

    public constructor(
        name: String,
        returns: PartiQLValueType,
        parameters: List<FnParameter>,
        description: String? = null,
        isDeterministic: Boolean = true,
        isNullable: Boolean = true,
        isNullCall: Boolean = false,
        isMissable: Boolean = true,
        isMissingCall: Boolean = true,
    ) : this(name, PType.fromPartiQLValueType(returns), parameters, description, isDeterministic, isNullable, isNullCall, isMissable, isMissingCall)

    /**
     * Symbolic name of this operator of the form NAME__INPUTS__RETURNS
     */
    public val specific: String = buildString {
        append(name.uppercase())
        append("__")
        append(parameters.joinToString("_") { it.type.toString() })
        append("__")
        append(returns)
    }

    /**
     * Use the symbolic name for easy debugging
     *
     * @return
     */
    override fun toString(): String = specific

    // Logic for writing a [FunctionSignature] using SQL `CREATE FUNCTION` syntax.

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

    public fun sql(): String = buildString {
        val fn = name.uppercase()
        val indent = "  "
        append("CREATE FUNCTION \"$fn\" (")
        if (parameters.isNotEmpty()) {
            val extent = parameters.maxOf { it.name.length }
            for (i in parameters.indices) {
                val p = parameters[i]
                val ws = (extent - p.name.length) + 1
                appendLine()
                append(indent).append(p.name.uppercase()).append(" ".repeat(ws)).append(p.type.toString())
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
}
