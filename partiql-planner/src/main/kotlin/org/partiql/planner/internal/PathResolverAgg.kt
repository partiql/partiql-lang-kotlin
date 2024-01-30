package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.toRuntimeType
import org.partiql.spi.connector.sql.builtins.Agg_ANY__BOOL__BOOL
import org.partiql.spi.connector.sql.builtins.Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.builtins.Agg_AVG__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.builtins.Agg_AVG__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.builtins.Agg_AVG__INT16__INT16
import org.partiql.spi.connector.sql.builtins.Agg_AVG__INT32__INT32
import org.partiql.spi.connector.sql.builtins.Agg_AVG__INT64__INT64
import org.partiql.spi.connector.sql.builtins.Agg_AVG__INT8__INT8
import org.partiql.spi.connector.sql.builtins.Agg_AVG__INT__INT
import org.partiql.spi.connector.sql.builtins.Agg_COUNT_STAR____INT32
import org.partiql.spi.connector.sql.builtins.Agg_COUNT__ANY__INT32
import org.partiql.spi.connector.sql.builtins.Agg_EVERY__BOOL__BOOL
import org.partiql.spi.connector.sql.builtins.Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.builtins.Agg_MAX__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.builtins.Agg_MAX__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.builtins.Agg_MAX__INT16__INT16
import org.partiql.spi.connector.sql.builtins.Agg_MAX__INT32__INT32
import org.partiql.spi.connector.sql.builtins.Agg_MAX__INT64__INT64
import org.partiql.spi.connector.sql.builtins.Agg_MAX__INT8__INT8
import org.partiql.spi.connector.sql.builtins.Agg_MAX__INT__INT
import org.partiql.spi.connector.sql.builtins.Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.builtins.Agg_MIN__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.builtins.Agg_MIN__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.builtins.Agg_MIN__INT16__INT16
import org.partiql.spi.connector.sql.builtins.Agg_MIN__INT32__INT32
import org.partiql.spi.connector.sql.builtins.Agg_MIN__INT64__INT64
import org.partiql.spi.connector.sql.builtins.Agg_MIN__INT8__INT8
import org.partiql.spi.connector.sql.builtins.Agg_MIN__INT__INT
import org.partiql.spi.connector.sql.builtins.Agg_SOME__BOOL__BOOL
import org.partiql.spi.connector.sql.builtins.Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.builtins.Agg_SUM__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.builtins.Agg_SUM__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.builtins.Agg_SUM__INT16__INT16
import org.partiql.spi.connector.sql.builtins.Agg_SUM__INT32__INT32
import org.partiql.spi.connector.sql.builtins.Agg_SUM__INT64__INT64
import org.partiql.spi.connector.sql.builtins.Agg_SUM__INT8__INT8
import org.partiql.spi.connector.sql.builtins.Agg_SUM__INT__INT
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.NULL

/**
 * Today, all aggregations are hard-coded into the grammar. We cannot implement user-defined aggregations until
 * the grammar and AST are updated appropriately. We should not have an aggregation node in the AST, just a call node.
 * During planning, we would then check if a call is an aggregation and translate the AST to the appropriate algebra.
 *
 * PartiQL.g4
 *
 * aggregate
 *     : func=COUNT PAREN_LEFT ASTERISK PAREN_RIGHT
 *     | func=(COUNT|MAX|MIN|SUM|AVG|EVERY|ANY|SOME) PAREN_LEFT setQuantifierStrategy? expr PAREN_RIGHT
 *     ;
 *
 */
@OptIn(FnExperimental::class, PartiQLValueExperimental::class)
internal object PathResolverAgg {

    @JvmStatic
    private val casts = CastTable.partiql()

    private val map = listOf(
        Agg_ANY__BOOL__BOOL,
        Agg_AVG__INT8__INT8,
        Agg_AVG__INT16__INT16,
        Agg_AVG__INT32__INT32,
        Agg_AVG__INT64__INT64,
        Agg_AVG__INT__INT,
        Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_AVG__FLOAT32__FLOAT32,
        Agg_AVG__FLOAT64__FLOAT64,
        Agg_COUNT__ANY__INT32,
        Agg_COUNT_STAR____INT32,
        Agg_EVERY__BOOL__BOOL,
        Agg_MAX__INT8__INT8,
        Agg_MAX__INT16__INT16,
        Agg_MAX__INT32__INT32,
        Agg_MAX__INT64__INT64,
        Agg_MAX__INT__INT,
        Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_MAX__FLOAT32__FLOAT32,
        Agg_MAX__FLOAT64__FLOAT64,
        Agg_MIN__INT8__INT8,
        Agg_MIN__INT16__INT16,
        Agg_MIN__INT32__INT32,
        Agg_MIN__INT64__INT64,
        Agg_MIN__INT__INT,
        Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_MIN__FLOAT32__FLOAT32,
        Agg_MIN__FLOAT64__FLOAT64,
        Agg_SOME__BOOL__BOOL,
        Agg_SUM__INT8__INT8,
        Agg_SUM__INT16__INT16,
        Agg_SUM__INT32__INT32,
        Agg_SUM__INT64__INT64,
        Agg_SUM__INT__INT,
        Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_SUM__FLOAT32__FLOAT32,
        Agg_SUM__FLOAT64__FLOAT64,
    ).map { it.signature }.groupBy { it.name }

    fun resolve(name: String, args: List<Rex>): Pair<AggSignature, Array<Ref.Cast?>>? {
        val candidates = map[name] ?: return null
        var hadMissingArg = false
        val parameters = args.mapIndexed { i, arg ->
            if (!hadMissingArg && arg.type.isMissable()) {
                hadMissingArg = true
            }
            arg.type.toRuntimeType()
        }
        return match(candidates, parameters)
    }

    private fun match(candidates: List<AggSignature>, args: List<PartiQLValueType>): Pair<AggSignature, Array<Ref.Cast?>>? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return candidate to arrayOfNulls(args.size)
            }
        }
        // 2. Look for best match.
        var match: Pair<AggSignature, Array<Ref.Cast?>>? = null
        for (candidate in candidates) {
            val m = candidate.match(args) ?: continue
            // TODO AggMatch comparison
            // if (match != null && m.exact < match.exact) {
            //     // already had a better match.
            //     continue
            // }
            match = m
        }
        // 3. Return best match or null
        return match
    }

    /**
     * Check if this function accepts the exact input argument types. Assume same arity.
     */
    private fun AggSignature.matches(args: List<PartiQLValueType>): Boolean {
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (a != p.type) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun AggSignature.match(args: List<PartiQLValueType>): Pair<AggSignature, Array<Ref.Cast?>>? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type == ANY -> continue
                // 3. Match NULL argument
                arg == NULL -> continue
                // 4. Check for a coercion
                else -> when (val coercion = casts.lookupCoercion(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return this to mapping
    }
}
