package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.typer.FnBuiltins.pAggs
import org.partiql.planner.internal.typer.FnBuiltins.pCasts
import org.partiql.planner.internal.typer.FnBuiltins.pFns
import org.partiql.planner.internal.typer.FnBuiltins.pOps
import org.partiql.spi.connector.ConnectorFunctions
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Function signature lookup by name.
 */
internal typealias FnMap<T> = Map<String, List<T>>

/**
 * This class is responsible for quickly looking up function signatures given a function name. We will expand upon
 * this to support the resolution of function names via the SQL path once ready.
 *
 * Note that this automatically includes the PartiQLMetadata as it would be wasteful to reproduce this every time.
 */
@PartiQLValueExperimental
internal class FnRegistry(private val metadata: Collection<ConnectorFunctions>) {

    /**
     * Calculate a queryable map of scalar function signatures.
     */
    private val uFns: FnMap<FunctionSignature.Scalar> = metadata.flatMap { it.functions }.toFnMap()

    /**
     * Calculate a queryable map of scalar function signatures from special forms.
     */
    private val uOps: FnMap<FunctionSignature.Scalar> = metadata.flatMap { it.operators }.toFnMap()

    /**
     * Calculate a queryable map of aggregation function signatures
     */
    private val uAggs: FnMap<FunctionSignature.Aggregation> = metadata.flatMap { it.aggregations }.toFnMap()

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     */
    internal fun lookup(ref: Fn.Unresolved): List<FunctionSignature.Scalar> {
        val name = getFnName(ref.identifier)
        // builtin
        val pMap = if (ref.isHidden) pOps else pFns
        val pFns = pMap.getOrDefault(name, emptyList())
        // user-defined
        val uMap = if (ref.isHidden) uFns else uOps
        val uFns = uMap[name]
        if (uFns.isNullOrEmpty()) {
            return pFns
        }
        return pFns + uFns
    }

    /**
     * Return a list of all aggregation function signatures matching the given identifier.
     */
    internal fun lookup(ref: Agg.Unresolved): List<FunctionSignature.Aggregation> {
        val name = getFnName(ref.identifier)
        // builtin
        val pFns = pAggs.getOrDefault(name, emptyList())
        // user-defined
        val uFns = uAggs[name]
        if (uFns.isNullOrEmpty()) {
            return pFns
        }
        return pFns + uFns
    }

    /**
     * Returns the CAST function if exists, else null.
     */
    internal fun lookupCoercion(operand: PartiQLValueType, target: PartiQLValueType): FunctionSignature.Scalar? {
        val i = operand.ordinal
        val j = target.ordinal
        val rel = pCasts.graph[i][j] ?: return null
        return if (rel.castType == CastType.COERCION) rel.castFn else null
    }

    internal fun isUnsafeCast(specific: String): Boolean = pCasts.unsafeCastSet.contains(specific)

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }
}
