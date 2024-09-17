package org.partiql.spi.function

import org.partiql.types.PType

/**
 *
 * TODO THIS WILL BE REPLACED BY THE `Function` INTERFACE IN A SUBSEQUENT PR.
 *  Right now the planner has a dependency on the signature which needs to be removed.
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
public data class FnSignature(
    @JvmField public val name: String,
    @JvmField public val returns: PType,
    @JvmField public val parameters: List<Parameter>,
    @JvmField public val description: String? = null,
    @JvmField public val isDeterministic: Boolean = true,
    @JvmField public val isNullable: Boolean = true,
    @JvmField public val isNullCall: Boolean = false,
    @JvmField public val isMissable: Boolean = true,
    @JvmField public val isMissingCall: Boolean = true,
) {

    /**
     * Symbolic name of this operator of the form NAME__INPUTS__RETURNS
     */
    public val specific: String = buildString {
        append(name.uppercase())
        append("__")
        append(parameters.joinToString("_") { it.getType().kind.toString() })
        append("__")
        append(returns)
    }

    /**
     * Use the symbolic name for easy debugging
     */
    override fun toString(): String = specific
}
