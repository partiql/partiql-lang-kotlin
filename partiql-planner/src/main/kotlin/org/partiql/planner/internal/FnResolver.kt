package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.toRuntimeTypeOrNull
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*

/**
 *
 * Resolution of static calls.
 *
 *  1. Sort all functions by resolution precedence.
 *  2. Check for a function accepting exactly the input argument types. If one exists, use it.
 *  3. Look for the best match
 *      a. Discard candidates whose arguments do not match or cannot be coerced.
 *      b. Check all candidates and keep those with the most exact matches.
 *
 * Resolution of dynamic calls.
 *
 *
 *
 * Reference https://www.postgresql.org/docs/current/typeconv-func.html
 */
@OptIn(FnExperimental::class, PartiQLValueExperimental::class)
internal object FnResolver {

    @JvmStatic
    private val casts = CastTable.partiql()

    /**
     * Resolution of either a static or dynamic function.
     *
     * @param variants
     * @param args
     * @return
     */
    fun resolve(variants: List<FnSignature>, args: List<StaticType>): FnMatch? {

        val candidates = variants
            .filter { it.parameters.size == args.size }
            .sortedWith(FnComparator)
            .ifEmpty { return null }

        val parameterPermutations = buildArgumentPermutations(args).mapNotNull { argList ->
            argList.map { arg ->
                // Skip over if we cannot convert type to runtime type.
                arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
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
            0 -> null
            1 -> orderedUniqueFunctions.first()
            else -> FnMatch.Dynamic(orderedUniqueFunctions, canReturnMissing)
        }
    }

    /**
     * Resolution of a static function.
     *
     * @param candidates
     * @param args
     * @return
     */
    private fun resolve(candidates: List<FnSignature>, args: FnArgs): FnMatch? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return FnMatch.Static(candidate, null)
            }
        }
        // 2. Look for best match.
        var match: FnMatch.Static? = null
        for (candidate in candidates) {
            val m = candidate.match(args) ?: continue
            if (match != null && m.exact < match.exact) {
                // already had a better match.
                continue
            }
            match = m
        }
        // 3. Return best match or null
        return match
    }

    /**
     * Check if this function accepts the exact input argument types. Assume same arity.
     */
    private fun FnSignature.matches(args: FnArgs): Boolean {
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (a != p.type) return true
        }
        return false
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun FnSignature.match(args: FnArgs): FnMatch.Static? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                a == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type == ANY -> continue
                // 3. Match NULL argument
                a == NULL -> continue
                // 4. Check for a coercion
                else -> {
                    val coercion = casts.lookupCoercion(a, p.type)
                    when (coercion) {
                        null -> return null // short-circuit
                        else -> mapping[i] = coercion
                    }
                }
            }
        }
        return FnMatch.Static(this, mapping)
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
}
