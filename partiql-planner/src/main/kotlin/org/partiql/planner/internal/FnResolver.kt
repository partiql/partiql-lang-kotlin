package org.partiql.planner.internal

import org.partiql.planner.internal.casts.Coercions
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.fn.Function
import org.partiql.types.PType.Kind

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

internal object FnResolver {

    /**
     * Resolution of either a static or dynamic function.
     *
     * TODO: How do we handle DYNAMIC?
     *
     * @param variants
     * @param args
     * @return
     */
    fun resolve(variants: List<Function>, args: List<CompilerType>): FnMatch? {
        val candidates = variants
            .filter { it.getParameters().size == args.size }
            .ifEmpty { return null }

        // 1. Look for exact match
        for (candidate in candidates) {
            if (candidate.matchesExactly(args)) {
                return FnMatch.Static(candidate, arrayOfNulls(args.size))
            }
        }

        // 2. If there are DYNAMIC arguments, return all candidates
        val isDynamic = args.any { it.kind == Kind.DYNAMIC }
        if (isDynamic) {
            val matches = match(candidates, args).ifEmpty { return null }
            val orderedMatches = matches.sortedWith(MatchResultComparator).map { it.match }
            return FnMatch.Dynamic(orderedMatches)
        }

        // 3. Look for the best match
        return resolveBestMatch(candidates, args)
    }

    private fun resolveBestMatch(candidates: List<Function>, args: List<CompilerType>): FnMatch.Static? {
        // 3. Discard functions that cannot be matched (via implicit coercion or exact matches)
        val invocableMatches = match(candidates, args).ifEmpty { return null }
        if (invocableMatches.size == 1) {
            return invocableMatches.first().match
        }

        // 4. Run through all candidates and keep those with the most exact matches on input types.
        val matches = matchOn(invocableMatches) { it.numberOfExactInputTypes }
        if (matches.size == 1) {
            return matches.first().match
        }

        // TODO: Do we care about preferred types? This is a PostgreSQL concept.
        // 5. Run through all candidates and keep those that accept preferred types (of the input data type's type category) at the most positions where type conversion will be required.

        // 6. Find the highest precedence one. NOTE: This is a remnant of the previous implementation. Whether we want
        //  to keep this is up to us.
        return matches.sortedWith(MatchResultComparator).first().match
    }

    /**
     * Resolution of a static function.
     *
     * @param candidates
     * @param args
     * @return
     */
    private fun match(candidates: List<Function>, args: List<CompilerType>): List<MatchResult> {
        val matches = mutableSetOf<MatchResult>()
        for (candidate in candidates) {
            val m = candidate.match(args) ?: continue
            matches.add(m)
        }
        return matches.toList()
    }

    private fun matchOn(candidates: List<MatchResult>, toCompare: (MatchResult) -> Int): List<MatchResult> {
        var mostExactMatches = 0
        val matches = mutableSetOf<MatchResult>()
        for (candidate in candidates) {
            when (toCompare(candidate).compareTo(mostExactMatches)) {
                -1 -> continue
                0 -> matches.add(candidate)
                1 -> {
                    mostExactMatches = toCompare(candidate)
                    matches.clear()
                    matches.add(candidate)
                }
                else -> error("CompareTo should never return outside of range [-1, 1]")
            }
        }
        return matches.toList()
    }

    /**
     * Check if this function accepts the exact input argument types. Assume same arity.
     */
    private fun Function.matchesExactly(args: List<CompilerType>): Boolean {
        val parameters = getParameters()
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (a != p.getType()) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun Function.match(args: List<CompilerType>): MatchResult? {
        val parameters = getParameters()
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        var exactInputTypes: Int = 0
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.getType() -> {
                    exactInputTypes++
                    continue
                }
                // 2. Match ANY parameter, no coercion needed
                p.getType().kind == Kind.DYNAMIC -> continue
                arg.kind == Kind.UNKNOWN -> continue
                // 3. Allow for ANY arguments
                arg.kind == Kind.DYNAMIC -> {
                    mapping[i] = Ref.Cast(arg, p.getType().toCType(), Ref.Cast.Safety.UNSAFE, true)
                }
                // 4. Check for a coercion
                else -> when (val coercion = Coercions.get(arg, p.getType())) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return MatchResult(
            FnMatch.Static(this, mapping),
            exactInputTypes,
        )
    }

    private class MatchResult(
        val match: FnMatch.Static,
        val numberOfExactInputTypes: Int,
    )

    private object MatchResultComparator : Comparator<MatchResult> {
        override fun compare(o1: MatchResult, o2: MatchResult): Int {
            return FnComparator.reversed().compare(o1.match.function, o2.match.function)
        }
    }
}
