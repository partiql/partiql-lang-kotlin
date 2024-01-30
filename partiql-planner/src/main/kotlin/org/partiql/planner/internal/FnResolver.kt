package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.typer.toRuntimeTypeOrNull
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.NULL

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

        val argPermutations = buildArgumentPermutations(args).mapNotNull { argList ->
            argList.map { arg ->
                // Skip over if we cannot convert type to runtime type.
                arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
            }
        }

        // Match candidates on all argument permutations
        val matches = argPermutations.mapNotNull { match(candidates, it) }

        // Remove duplicates while maintaining order (precedence).
        val orderedUniqueFunctions = matches.toSet().toList()

        //
        return when (orderedUniqueFunctions.size) {
            0 -> null
            1 -> orderedUniqueFunctions.first()
            else -> FnMatch.Dynamic(orderedUniqueFunctions)
        }
    }

    /**
     * Resolution of a static function.
     *
     * @param candidates
     * @param args
     * @return
     */
    private fun match(candidates: List<FnSignature>, args: List<PartiQLValueType>): FnMatch.Static? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return FnMatch.Static(candidate, arrayOfNulls(args.size))
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
    private fun FnSignature.matches(args: List<PartiQLValueType>): Boolean {
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
    private fun FnSignature.match(args: List<PartiQLValueType>): FnMatch.Static? {
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
