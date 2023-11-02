package org.partiql.planner.typer

import org.partiql.plan.Agg
import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.plan.Rex
import org.partiql.planner.Header
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * Function signature lookup by name.
 */
internal typealias FnMap<T> = Map<String, List<T>>

/**
 * Function arguments list. The planner is responsible for mapping arguments to parameters.
 */
internal typealias Args = List<FunctionParameter>

/**
 * Parameter mapping list tells the planner where to insert implicit casts. Null is the identity.
 */
internal typealias Mapping = List<FunctionSignature.Scalar?>

/**
 * Tells us which function matched, and how the arguments are mapped.
 */
internal class Match<T : FunctionSignature>(
    public val signature: T,
    public val mapping: Mapping,
)

/**
 * Result of attempting to match an unresolved function.
 */
internal sealed class FnMatch<T : FunctionSignature> {

    /**
     * 7.1 Inputs with wrong types
     *      It follows that all functions return MISSING when one of their inputs is MISSING
     *
     * @property signature
     * @property mapping
     * @property isMissable TRUE when anyone of the arguments _could_ be MISSING. We *always* propagate MISSING.
     */
    public data class Ok<T : FunctionSignature>(
        public val signature: T,
        public val mapping: Mapping,
        public val isMissable: Boolean,
    ) : FnMatch<T>()

    public data class Error<T : FunctionSignature>(
        public val identifier: Identifier,
        public val args: List<Rex>,
        public val candidates: List<FunctionSignature>,
    ) : FnMatch<T>()
}

/**
 * Logic for matching signatures to arguments — this class contains all cast/coercion logic. In my opinion, casts
 * and coercions should come along with the type lattice. Considering we don't really have this, it is simple enough
 * at the moment to keep that information (derived from the current TypeLattice) with the [FnResolver].
 */
@OptIn(PartiQLValueExperimental::class)
internal class FnResolver(private val headers: List<Header>) {

    /**
     * All headers use the same type lattice (we don't have a design for plugging type systems at the moment).
     */
    private val types = TypeLattice.partiql()

    /**
     * Calculate a queryable map of scalar function signatures.
     */
    private val functions: FnMap<FunctionSignature.Scalar>

    /**
     * Calculate a queryable map of scalar function signatures from special forms.
     */
    private val operators: FnMap<FunctionSignature.Scalar>

    /**
     * Calculate a queryable map of aggregation function signatures
     */
    private val aggregations: FnMap<FunctionSignature.Aggregation>

    /**
     * A place to quickly lookup a cast can return missing; lookup by "SPECIFIC"
     */
    private val unsafeCastSet: Set<String>

    init {
        val (casts, unsafeCasts) = casts()
        unsafeCastSet = unsafeCasts
        // combine all header definitions
        val fns = headers.flatMap { it.functions }
        functions = fns.toFnMap()
        operators = (headers.flatMap { it.operators } + casts).toFnMap()
        aggregations = headers.flatMap { it.aggregations }.toFnMap()
    }

    /**
     * Group list of [FunctionSignature] by name.
     */
    private fun <T : FunctionSignature> List<T>.toFnMap(): FnMap<T> = this
        .distinctBy { it.specific }
        .sortedWith(fnPrecedence)
        .groupBy { it.name }

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] scalar function catalog.
     */
    public fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Scalar> {
        val candidates = lookup(fn)
        var hadMissingArg = false
        val parameters = args.mapIndexed { i, arg ->
            if (!hadMissingArg && arg.type.isMissable()) {
                hadMissingArg = true
            }
            FunctionParameter("arg-$i", arg.type.toRuntimeType())
        }
        val match = match(candidates, parameters)
        return when (match) {
            null -> FnMatch.Error(fn.identifier, args, candidates)
            else -> {
                val isMissable = hadMissingArg || isUnsafeCast(match.signature.specific) || match.signature.isMissable
                FnMatch.Ok(match.signature, match.mapping, isMissable)
            }
        }
    }

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] aggregation function catalog.
     */
    public fun resolveAgg(agg: Agg.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Aggregation> {
        val candidates = lookup(agg)
        var hadMissingArg = false
        val parameters = args.mapIndexed { i, arg ->
            if (!hadMissingArg && arg.type.isMissable()) {
                hadMissingArg = true
            }
            FunctionParameter("arg-$i", arg.type.toRuntimeType())
        }
        val match = match(candidates, parameters)
        return when (match) {
            null -> FnMatch.Error(agg.identifier, args, candidates)
            else -> {
                val isMissable = hadMissingArg || isUnsafeCast(match.signature.specific)
                FnMatch.Ok(match.signature, match.mapping, isMissable)
            }
        }
    }

    /**
     * Functions are sorted by precedence (which is not rigorously defined/specified at the moment).
     */
    private fun <T : FunctionSignature> match(signatures: List<T>, args: Args): Match<T>? {
        for (signature in signatures) {
            val mapping = match(signature, args)
            if (mapping != null) {
                return Match(signature, mapping)
            }
        }
        return null
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * TODO we need to constrain the allowable runtime types for an ANY typed parameter.
     */
    fun match(signature: FunctionSignature, args: Args): Mapping? {
        if (signature.parameters.size != args.size) {
            return null
        }
        val mapping = ArrayList<FunctionSignature.Scalar?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                // 1. Exact match
                a.type == p.type -> mapping.add(null)
                // 2. Match ANY, no coercion needed
                p.type == ANY -> mapping.add(null)
                // 3. Match NULL argument
                a.type == NULL -> mapping.add(null)
                // 4. Check for a coercion
                else -> {
                    val coercion = lookupCoercion(a.type, p.type)
                    when (coercion) {
                        null -> return null // short-circuit
                        else -> mapping.add(coercion)
                    }
                }
            }
        }
        // we made a match
        return mapping
    }

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     */
    private fun lookup(ref: Fn.Unresolved): List<FunctionSignature.Scalar> {
        val name = getFnName(ref.identifier)
        return when (ref.isHidden) {
            true -> operators.getOrDefault(name, emptyList())
            else -> functions.getOrDefault(name, emptyList())
        }
    }

    /**
     * Return a list of all aggregation function signatures matching the given identifier.
     */
    private fun lookup(ref: Agg.Unresolved): List<FunctionSignature.Aggregation> {
        val name = getFnName(ref.identifier)
        return aggregations.getOrDefault(name, emptyList())
    }

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }

    // ====================================
    //  CASTS and COERCIONS
    // ====================================

    /**
     * Returns the CAST function if exists, else null.
     */
    private fun lookupCoercion(valueType: PartiQLValueType, targetType: PartiQLValueType): FunctionSignature.Scalar? {
        if (!types.canCoerce(valueType, targetType)) {
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
    private fun isUnsafeCast(specific: String): Boolean = unsafeCastSet.contains(specific)

    /**
     * Generate all CAST functions from the given lattice.
     *
     * @return Pair(0) is the function list, Pair(1) represents the unsafe cast specifics
     */
    private fun casts(): Pair<List<FunctionSignature.Scalar>, Set<String>> {
        val casts = mutableListOf<FunctionSignature.Scalar>()
        val unsafeCastSet = mutableSetOf<String>()
        for (t1 in types.types) {
            for (t2 in types.types) {
                val r = types.graph[t1.ordinal][t2.ordinal]
                if (r != null) {
                    val fn = cast(t1, t2)
                    casts.add(fn)
                    if (r.cast == CastType.UNSAFE) unsafeCastSet.add(fn.specific)
                }
            }
        }
        return casts to unsafeCastSet
    }

    /**
     * Define CASTS with some mangled name; CAST(x AS T) -> cast_t(x)
     *
     * CAST(x AS INT8) -> cast_int64(x)
     *
     * But what about parameterized types? Are the parameters dropped in casts, or do parameters become arguments?
     */
    private fun castName(type: PartiQLValueType) = "cast_${type.name.lowercase()}"

    internal fun cast(operand: PartiQLValueType, target: PartiQLValueType) =
        FunctionSignature.Scalar(
            name = castName(target),
            returns = target,
            isNullCall = true,
            isNullable = false,
            parameters = listOf(
                FunctionParameter("value", operand),
            )
        )

    companion object {

        // ====================================
        //  SORTING
        // ====================================

        // Function precedence comparator
        // 1. Fewest args first
        // 2. Parameters are compared left-to-right
        @JvmStatic
        private val fnPrecedence = Comparator<FunctionSignature> { fn1, fn2 ->
            // Compare number of arguments
            if (fn1.parameters.size != fn2.parameters.size) {
                return@Comparator fn1.parameters.size - fn2.parameters.size
            }
            // Compare operand type precedence
            for (i in fn1.parameters.indices) {
                val p1 = fn1.parameters[i]
                val p2 = fn2.parameters[i]
                val comparison = p1.compareTo(p2)
                if (comparison != 0) return@Comparator comparison
            }
            // unreachable?
            0
        }

        private fun FunctionParameter.compareTo(other: FunctionParameter): Int =
            comparePrecedence(this.type, other.type)

        private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
            if (t1 == t2) return 0
            val p1 = precedence[t1]!!
            val p2 = precedence[t2]!!
            return p1 - p2
        }

        // This simply describes some precedence for ordering functions.
        // This is not explicitly defined in the PartiQL Specification!!
        // This does not imply the ability to CAST; this defines function resolution behavior.
        private val precedence: Map<PartiQLValueType, Int> = listOf(
            NULL,
            MISSING,
            BOOL,
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
            DECIMAL,
            FLOAT32,
            FLOAT64,
            CHAR,
            STRING,
            CLOB,
            SYMBOL,
            BINARY,
            BYTE,
            BLOB,
            DATE,
            TIME,
            TIMESTAMP,
            INTERVAL,
            LIST,
            SEXP,
            BAG,
            STRUCT,
            ANY,
        ).mapIndexed { precedence, type -> type to precedence }.toMap()
    }
}
