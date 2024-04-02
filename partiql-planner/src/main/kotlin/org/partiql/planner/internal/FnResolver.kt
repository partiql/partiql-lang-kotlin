package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.shape.PShape
import org.partiql.shape.PShape.Companion.allTypes
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DynamicType
import org.partiql.value.PartiQLType

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
@OptIn(FnExperimental::class)
internal object FnResolver {

    @JvmStatic
    private val casts = CastTable.partiql

    /**
     * Resolution of either a static or dynamic function.
     *
     * @param variants
     * @param args
     * @return
     */
    fun resolve(variants: List<FnSignature>, args: List<PShape>): FnMatch? {

        val candidates = variants
            .filter { it.parameters.size == args.size }
            .sortedWith(FnComparator)
            .ifEmpty { return null }

        val argPermutations = buildArgumentPermutations(args)

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
    private fun match(candidates: List<FnSignature>, args: List<PartiQLType>): FnMatch.Static? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return FnMatch.Static(candidate, arrayOfNulls(args.size))
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
    private fun FnSignature.matches(args: List<PartiQLType>): Boolean {
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
    private fun FnSignature.match(args: List<PartiQLType>): FnMatch.Static? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type is DynamicType -> continue
                else -> when (val coercion = casts.lookupCoercion(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return FnMatch.Static(this, mapping)
    }

    private fun buildArgumentPermutations(args: List<PShape>): List<List<PartiQLType>> {
        val flattenedArgs = args.map { it.allTypes().toList() }
        return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
    }

    private fun buildArgumentPermutations(
        args: List<List<PartiQLType>>,
        accumulator: List<PartiQLType>,
    ): List<List<PartiQLType>> {
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
