package org.partiql.planner.internal.typer

import org.partiql.planner.internal.Header
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.types.AnyOfType
import org.partiql.types.NullType
import org.partiql.types.StaticType
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
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
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

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates an ordered list of potentially applicable functions to dispatch dynamically.
     * @property isMissable TRUE when the argument permutations may not definitively invoke one of the candidates. You
     * can think of [isMissable] as being the same as "not exhaustive". For example, if we have ABS(INT | STRING), then
     * this function call [isMissable] because there isn't an `ABS(STRING)` function signature AKA we haven't exhausted
     * all the arguments. On the other hand, take an "exhaustive" scenario: ABS(INT | DEC). In this case, [isMissable]
     * is false because we have functions for each potential argument AKA we have exhausted the arguments.
     */
    public data class Dynamic<T : FunctionSignature>(
        public val candidates: List<Ok<T>>,
        public val isMissable: Boolean
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
internal class FnResolver(private val header: Header) {

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
        val fns = header.functions
        functions = fns.toFnMap()
        operators = (header.operators + casts).toFnMap()
        aggregations = header.aggregations.toFnMap()
    }

    /**
     * Group list of [FunctionSignature] by name.
     */
    private fun <T : FunctionSignature> List<T>.toFnMap(): FnMap<T> = this
        .distinctBy { it.specific }
        .sortedWith(fnPrecedence)
        .groupBy { it.name }

    /**
     * Resolution of either a static or dynamic function.
     *
     * @param variants
     * @param args
     * @return
     */
    fun resolve(variants: List<FunctionSignature.Scalar>, args: List<StaticType>): FnMatch<FunctionSignature.Scalar>? {

        val candidates = variants
            .filter { it.parameters.size == args.size }
            .sortedWith(FnComparator)
            .ifEmpty { return null }

        val argPermutations = buildArgumentPermutations(args).mapNotNull { argList ->
            argList.map { arg ->
                // Skip over if we cannot convert type to runtime type.
                arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
            }
        }

        // Match candidates on all argument permutations
        var exhaustive = true
        val matches = argPermutations.mapNotNull {
            val m = match(candidates, it)
            if (m == null) {
                // we had a branch whose arguments did not match a static call
                exhaustive = false
            }
            m
        }

        // Order based on original candidate function ordering
        val orderedUniqueMatches = matches.toSet().toList()
        val orderedCandidates = candidates.flatMap { candidate ->
            orderedUniqueMatches.filter { it.signature == candidate }
        }

        // Static call iff only one match for every branch
        val n = orderedCandidates.size
        return when {
            n == 0 -> null
            n == 1 && exhaustive -> orderedCandidates.first()
            else -> FnMatch.Dynamic(orderedCandidates, exhaustive)
        }
    }

    /**
     * Resolution of a static function.
     *
     * @param candidates
     * @param args
     * @return
     */
    private fun match(candidates: List<FunctionSignature.Scalar>, args: List<PartiQLValueType>): FnMatch.Ok<FunctionSignature.Scalar>? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                val isMissable = candidate.isMissable || (candidate.isMissingCall && args.any { it == MISSING })
                return FnMatch.Ok(candidate, List(args.size) { null }, isMissable)
            }
        }
        // 2. Look for best match (for now, first match).
        for (candidate in candidates) {
            val m = candidate.match(args)
            if (m != null) {
                return m
            }
            // if (match != null && m.exact < match.exact) {
            //     // already had a better match.
            //     continue
            // }
            // match = m
        }
        // 3. No match, return null
        return null
    }

    /**
     * Check if this function accepts the exact input argument types. Assume same arity.
     */
    private fun FunctionSignature.Scalar.matches(args: List<PartiQLValueType>): Boolean {
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
    private fun FunctionSignature.Scalar.match(args: List<PartiQLValueType>): FnMatch.Ok<FunctionSignature.Scalar>? {
        val mapping = arrayOfNulls<FunctionSignature.Scalar?>(args.size)
        var hasUnsafeCasts = false
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type == ANY -> continue
                // 3. Check for a coercion
                else -> when (val coercion = lookupCoercion(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> {
                        hasUnsafeCasts = hasUnsafeCasts || isUnsafeCast(coercion.specific)
                        mapping[i] = coercion
                    }
                }
            }
        }
        val isMissable = this.isMissable || (this.isMissingCall && (args.any { it == MISSING } || hasUnsafeCasts))
        return FnMatch.Ok(this, mapping.toList(), isMissable = isMissable)
    }

    fun resolveFnScalar(path: Fn.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Scalar> {
        val variants = lookup(path)
        if (variants.isEmpty()) {
            return FnMatch.Error(
                path.identifier,
                args,
                variants
            )
        }
        // Invoke FnResolver to determine if we made a match
        return resolve(variants, args.map { it.type }) ?: FnMatch.Error(
            path.identifier,
            args,
            variants
        )
    }

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] scalar function catalog.
     */
    public fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Scalar> {
        val candidates = lookup(fn)
        var canReturnMissing = false
        val parameterPermutations = buildArgumentPermutations(args.map { it.type }).mapNotNull { argList ->
            argList.mapIndexed { i, arg ->
                if (arg.isMissable()) {
                    canReturnMissing = true
                }
                // Skip over if we cannot convert type to runtime type.
                val argType = arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
                FunctionParameter("arg-$i", argType)
            }
        }
        val potentialFunctions = parameterPermutations.mapNotNull { parameters ->
            when (val match = match(candidates, parameters)) {
                null -> {
                    canReturnMissing = true
                    null
                }
                else -> {
                    val isMissable = canReturnMissing || isUnsafeCast(match.signature.specific)
                    FnMatch.Ok(match.signature, match.mapping, isMissable)
                }
            }
        }
        // Remove duplicates while maintaining order (precedence).
        val orderedUniqueFunctions = potentialFunctions.toSet().toList()
        return when (orderedUniqueFunctions.size) {
            0 -> FnMatch.Error(fn.identifier, args, candidates)
            1 -> orderedUniqueFunctions.first()
            else -> FnMatch.Dynamic(orderedUniqueFunctions, canReturnMissing)
        }
    }

    private fun buildArgumentPermutations(args: List<StaticType>): List<List<StaticType>> {
        val flattenedArgs = args.map {
            if (it is AnyOfType) {
                it.flatten().allTypes.filter { it !is NullType }
            } else {
                it.flatten().allTypes
            }
        }
        return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
    }

    private fun buildArgumentPermutations(
        args: List<List<StaticType>>,
        accumulator: List<StaticType>,
    ): List<List<StaticType>> {
        if (args.isEmpty()) {
            return listOf(accumulator)
        }
        val first = args.first()
        val rest = when (args.size) {
            1 -> emptyList()
            else -> args.subList(1, args.size)
        }
        return buildList {
            first.forEach { argSubType ->
                addAll(buildArgumentPermutations(rest, accumulator + listOf(argSubType)))
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
     *
     * This function first attempts to find all possible match for given args
     * If there are multiple matches, then
     *   - return the matches that requires the lowest number of coercion
     *          - This is to match edges like concat(symbol, symbol) which should return symbol
     *             but because string has higher precedence,
     *             we also would have concat(cast(symbol as string), cast(symbol as string))
     *             added to the map first.
     *   - return the matches which has the highest argument precedence.
     */
    private fun <T : FunctionSignature> match(signatures: List<T>, args: Args): Match<T>? {
        val candidates = mutableListOf<Match<T>>()
        for (signature in signatures) {
            val mapping = match(signature, args)
            if (mapping != null) {
                candidates.add(Match(signature, mapping))
            }
        }

        // Sorted By is stable, we don't have to resort based on parameter type precedence
        candidates.sortBy { it.mapping.filterNotNull().size }
        return candidates.firstOrNull()
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
        val handlesMissing = when (signature) {
            is FunctionSignature.Aggregation -> true
            is FunctionSignature.Scalar -> !signature.isMissingCall
        }
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                // 1. Exact match
                a.type == p.type -> mapping.add(null)
                // TODO
                a.type == MISSING && handlesMissing -> mapping.add(null)
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
            parameters = listOf(
                FunctionParameter("value", operand),
            ),
            isNullable = false,
            isNullCall = true
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
            DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
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
