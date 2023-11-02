package org.partiql.planner

import org.partiql.planner.typer.TypeLattice
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * A (temporary) place for function definitions; there are whispers of loading this as information_schema.
 */
@OptIn(PartiQLValueExperimental::class)
public abstract class Header {

    /**
     * Definition namespace e.g. partiql, spark, redshift, ...
     */
    abstract val namespace: String

    /**
     * Scalar function signatures available via call syntax.
     */
    open val functions: List<FunctionSignature.Scalar> = emptyList()

    /**
     * Hidden scalar function signatures available via operator or special form syntax.
     */
    open val operators: List<FunctionSignature.Scalar> = emptyList()

    /**
     * Aggregation function signatures.
     */
    open val aggregations: List<FunctionSignature.Aggregation> = emptyList()

    /**
     * Type relationships; this is primarily a helper for defining operators.
     */
    internal val types: TypeLattice = TypeLattice.partiql()

    /**
     * Dump the Header as SQL commands
     *
     * For functions, output CREATE FUNCTION statements.
     */
    override fun toString(): String = buildString {
        (functions + operators + aggregations).groupBy { it.name }.forEach {
            appendLine("-- [${it.key}] ---------")
            appendLine()
            it.value.forEach { fn -> appendLine(fn) }
            appendLine()
        }
    }

    // ====================================
    //  HELPERS
    // ====================================

    companion object {

        @JvmStatic
        internal fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType, isMissable: Boolean = false) =
            FunctionSignature.Scalar(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("value", value)),
                isNullCall = true,
                isNullable = false,
                isMissable = isMissable
            )

        @JvmStatic
        internal fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType, isMissable: Boolean = false) =
            FunctionSignature.Scalar(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("lhs", lhs), FunctionParameter("rhs", rhs)),
                isNullCall = true,
                isNullable = false,
                isMissable = isMissable
            )
    }
}
