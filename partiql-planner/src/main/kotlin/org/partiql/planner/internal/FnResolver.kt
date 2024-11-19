package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.function.Function
import org.partiql.types.PType
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
                val fn = candidate.getInstance(args.toTypedArray()) ?: error("This shouldn't have happened. Matching exactly should produce a function instance.")
                return FnMatch.Static(fn, arrayOfNulls(args.size))
            }
        }

        // 2. If there are DYNAMIC arguments, return all candidates
        val isDynamic = args.any { it.kind == Kind.DYNAMIC }
        if (isDynamic) {
            val orderedMatches = candidates.sortedWith(FnComparator)
            return FnMatch.Dynamic(orderedMatches)
        }

        // 3. Look for the best match
        return resolveBestMatch(candidates, args)
    }

    private fun resolveBestMatch(candidates: List<Function>, args: List<CompilerType>): FnMatch.Static? {
        // 3. Discard functions that cannot be matched (via implicit coercion or exact matches)
        val invocableMatches = match(candidates, args).ifEmpty { return null }
        if (invocableMatches.size == 1) {
            val match = invocableMatches.first()
            val fn = match.match.getInstance(args.toTypedArray()) ?: return null
            return FnMatch.Static(fn, match.mapping)
        }

        // 4. Run through all candidates and keep those with the most exact matches on input types.
        val matches = matchOn(invocableMatches) { it.numberOfExactInputTypes }
        if (matches.size == 1) {
            val match = matches.first()
            val fn = match.match.getInstance(args.toTypedArray()) ?: return null
            return FnMatch.Static(fn, match.mapping)
        }

        // TODO: Do we care about preferred types? This is a PostgreSQL concept.
        // 5. Run through all candidates and keep those that accept preferred types (of the input data type's type category) at the most positions where type conversion will be required.

        // 6. Find the highest precedence one. NOTE: This is a remnant of the previous implementation. Whether we want
        //  to keep this is up to us.
        val match = matches.sortedWith(MatchResultComparator).first()
        val fn = match.match
        val instance = fn.getInstance(args.toTypedArray()) ?: return null
        return FnMatch.Static(instance, match.mapping)
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
        val instance = getInstance(args.toTypedArray()) ?: return false
        val parameters = instance.parameters
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (p != a) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the coercions if necessary.
     *
     * @param args
     * @return
     */
    private fun Function.match(args: List<CompilerType>): MatchResult? {
        val instance = this.getInstance(args.toTypedArray()) ?: return null
        val parameters = instance.parameters
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        var exactInputTypes = 0
        for (i in args.indices) {
            val a = args[i]
            if (a.kind == Kind.UNKNOWN) {
                continue // skip unknown arguments
            }
            // check match
            val p = parameters[i]
            when {
                p == a -> exactInputTypes++
                else -> mapping[i] = coercion(a, p) ?: return null
            }
        }
        return MatchResult(
            this,
            mapping,
            exactInputTypes,
        )
    }

    private fun coercion(arg: PType, target: PType): Ref.Cast? {
        return when (CoercionFamily.canCoerce(arg, target)) {
            true -> Ref.Cast(arg.toCType(), target.toCType(), Ref.Cast.Safety.COERCION, true)
            false -> return null
        }
    }

    private class MatchResult(
        val match: Function,
        val mapping: Array<Ref.Cast?>,
        val numberOfExactInputTypes: Int,
    )

    private object MatchResultComparator : Comparator<MatchResult> {
        override fun compare(o1: MatchResult, o2: MatchResult): Int {
            return FnComparator.reversed().compare(o1.match, o2.match)
        }
    }
}
