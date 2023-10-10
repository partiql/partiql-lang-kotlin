package org.partiql.types.function

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
 */
@OptIn(PartiQLValueExperimental::class)
public sealed class FunctionSignature(
    @JvmField public val name: String,
    @JvmField public val returns: PartiQLValueType,
    @JvmField public val parameters: List<FunctionParameter>,
    @JvmField public val description: String? = null,
    @JvmField public val isNullable: Boolean = true,
    @JvmField public val isHidden: Boolean = false,
) {

    /**
     * Symbolic name of this operator of the form NAME__INPUTS__RETURNS
     */
    public val specific: String = buildString {
        append(name.uppercase())
        append("__")
        append(parameters.joinToString("_") { it.type.name })
        append("__")
        append(returns.name)
    }

    /**
     * Use the symbolic name for easy debugging
     *
     * @return
     */
    override fun toString(): String = specific

    /**
     * Represents the signature of a PartiQL scalar function.
     *
     * @property isDeterministic    Flag indicating this function always produces the same output given the same input.
     * @property isNullCall         Flag indicating if any of the call arguments is NULL, then return NULL.
     * @property isNullable         Flag indicating this function's operator may return a NULL value.
     * @property isHidden           Flag indicating this functional is hidden from SQL call syntax
     * @constructor
     */
    public class Scalar(
        name: String,
        returns: PartiQLValueType,
        parameters: List<FunctionParameter>,
        description: String? = null,
        isNullable: Boolean = true,
        isHidden: Boolean = false,
        @JvmField public val isDeterministic: Boolean = true,
        @JvmField public val isNullCall: Boolean = false,
    ) : FunctionSignature(name, returns, parameters, description, isNullable, isHidden) {

        override fun equals(other: Any?): Boolean {
            if (other !is Scalar) return false
            if (
                other.name != name ||
                other.returns != returns ||
                other.parameters.size != parameters.size ||
                other.isDeterministic != isDeterministic ||
                other.isNullCall != isNullCall ||
                other.isNullable != isNullable ||
                other.isHidden != isHidden
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
            result = 31 * result + isNullCall.hashCode()
            result = 31 * result + isNullable.hashCode()
            result = 31 * result + isHidden.hashCode()
            result = 31 * result + (description?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * Represents the signature of a PartiQL aggregation function.
     *
     * @property isDecomposable     Flag indicating this aggregation can be decomposed
     * @property isHidden           Flag indicating this functional is hidden from SQL call syntax
     * @constructor
     */
    public class Aggregation(
        name: String,
        returns: PartiQLValueType,
        parameters: List<FunctionParameter>,
        description: String? = null,
        isNullable: Boolean = true,
        isHidden: Boolean = false,
        @JvmField public val isDecomposable: Boolean = true,
    ) : FunctionSignature(name, returns, parameters, description, isNullable, isHidden) {

        override fun equals(other: Any?): Boolean {
            if (other !is Aggregation) return false
            if (
                other.name != name ||
                other.returns != returns ||
                other.parameters.size != parameters.size ||
                other.isDecomposable != isDecomposable ||
                other.isHidden != isHidden
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
            result = 31 * result + isDecomposable.hashCode()
            result = 31 * result + isHidden.hashCode()
            result = 31 * result + (description?.hashCode() ?: 0)
            return result
        }
    }

    //
    // // Logic for writing a [FunctionSignature] using SQL `CREATE FUNCTION` syntax.
    //
    // /**
    //  * SQL-99 p.542 <deterministic characteristic>
    //  */
    // private val deterministicCharacteristic = when (isDeterministic) {
    //     true -> "DETERMINISTIC"
    //     else -> "NOT DETERMINISTIC"
    // }
    //
    // /**
    //  * SQL-99 p.543 <null-call clause>
    //  */
    // private val nullCallClause = when (isNullCall) {
    //     true -> "RETURNS NULL ON NULL INPUT"
    //     else -> "CALLED ON NULL INPUT"
    // }
    //
    // internal fun sql(): String = buildString {
    //     val fn = name.uppercase()
    //     val indent = "  "
    //     append("CREATE FUNCTION \"$fn\" (")
    //     if (parameters.isNotEmpty()) {
    //         val extent = parameters.maxOf { it.name.length }
    //         for (i in parameters.indices) {
    //             val p = parameters[i]
    //             val ws = (extent - p.name.length) + 1
    //             appendLine()
    //             append(indent).append(p.name.uppercase()).append(" ".repeat(ws)).append(p.type.name)
    //             if (i != parameters.size - 1) append(",")
    //         }
    //     }
    //     appendLine(" )")
    //     append(indent).appendLine("RETURNS $returns")
    //     append(indent).appendLine("SPECIFIC $specific")
    //     append(indent).appendLine(deterministicCharacteristic)
    //     append(indent).appendLine(nullCallClause)
    //     append(indent).appendLine("RETURN $fn ( ${parameters.joinToString { it.name.uppercase() }} ) ;")
    // }
}
