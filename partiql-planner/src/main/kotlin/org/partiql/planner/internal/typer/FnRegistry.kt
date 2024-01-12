package org.partiql.planner.internal.typer

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * The [FnRegistry] is res
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal class FnRegistry(
    private val session: PartiQLPlanner.Session,
    private val catalogs: Map<String, ConnectorMetadata>,
) {

    companion object {

        /**
         * PartiQL type relationships and casts.
         */
        @JvmStatic
        internal val casts = TypeCasts.partiql()
    }

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     *
     * TODO add support for qualified function identifiers.
     */
    internal fun lookup(ref: Fn.Unresolved): List<FnSignature.Scalar> {
        val name = getFnName(ref.identifier)

        return when (ref.isHidden) {
            true -> operators.getOrDefault(name, emptyList())
            else -> functions.getOrDefault(name, emptyList())
        }
    }

    /**
     * Return a list of all aggregation function signatures matching the given identifier.
     */
    internal fun lookup(ref: Agg.Unresolved): List<FnSignature.Aggregation> {
        val name = getFnName(ref.identifier)
        return aggregations.getOrDefault(name, emptyList())
    }

    // ====================================
    //  CASTS and COERCIONS
    // ====================================

    /**
     * Returns the CAST function if exists, else null.
     */
    internal fun lookupCoercion(valueType: PartiQLValueType, targetType: PartiQLValueType): FnSignature.Scalar? {
        if (!casts.canCoerce(valueType, targetType)) {
            return null
        }
        val name = castName(targetType)
        val casts = operators.getOrDefault(name, emptyList())
        for (cast in casts) {
            if (cast.parameters.isEmpty()) {
                break // should be unreachable
            }
            if (valueType == cast.parameters[0].type) return cast
        }
        return null
    }

    /**
     * Easy lookup of whether this CAST can return MISSING.
     */
    public fun isUnsafeCast(specific: String): Boolean = casts.unsafeCastSet.contains(specific)

    /**
     * Define CASTS with some mangled name; CAST(x AS T) -> cast_t(x)
     *
     * CAST(x AS INT8) -> cast_int64(x)
     *
     * But what about parameterized types? Are the parameters dropped in casts, or do parameters become arguments?
     */
    private fun castName(type: PartiQLValueType) = "cast_${type.name.lowercase()}"
}