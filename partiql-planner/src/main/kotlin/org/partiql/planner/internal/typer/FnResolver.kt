package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.NULL

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
internal class FnResolver(private val metadata: Collection<ConnectorMetadata>) {

    /**
     * FnRegistry holds
     */
    private val registry = FnRegistry(metadata)

    /**
     * Leverages a [FnResolver] to find a matching function defined in ConnectorFunctions.
     */
    public fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Scalar> {
        val candidates = registry.lookup(fn)
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
                    val isMissable = canReturnMissing || registry.isUnsafeCast(match.signature.specific)
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
        val flattenedArgs = args.map { it.flatten().allTypes }
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
        val candidates = registry.lookup(agg)
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
                val isMissable = hadMissingArg || registry.isUnsafeCast(match.signature.specific)
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
                    val coercion = registry.lookupCoercion(a.type, p.type)
                    when (coercion) {
                        null -> return null // short-circuit
                        else -> mapping.add(coercion)
                    }
                }
            }
        }
        // if all elements requires casting, then no match
        // because there must be another function definition that requires no casting
        return if (mapping.isEmpty() || mapping.contains(null)) {
            // we made a match
            mapping
        } else {
            null
        }
    }

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     */
    internal fun lookup(ref: Fn.Unresolved): List<FunctionSignature.Scalar> {
        val name = getFnName(ref.identifier)
        // builtin
        val  = if (ref.isHidden) pOps else pFns
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
        val rel = casts.graph[i][j] ?: return null
        return if (rel.castType == CastType.COERCION) rel.castFn else null
    }

    internal fun isUnsafeCast(specific: String): Boolean = casts.unsafeCastSet.contains(specific)

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }

    companion object {

        @JvmStatic
        private val casts = TypeCasts.partiql()
    }
}
