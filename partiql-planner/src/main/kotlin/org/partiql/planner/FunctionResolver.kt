package org.partiql.planner

import org.partiql.plan.Ref
import org.partiql.planner.internal.FnComparator
import org.partiql.planner.internal.casts.Coercions
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
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
@OptIn(FnExperimental::class)
public object FunctionResolver {

    /**
     * Resolution of either a static or dynamic function.
     *
     * TODO: How do we handle DYNAMIC?
     *
     * @param variants
     * @param args
     * @return
     */
    public fun resolve(variants: List<FnSignature>, args: List<PType>): FnMatch? {
        val candidates = variants
            .mapIndexed { index, fnSignature -> Candidate(index, fnSignature) }
            .filter { it.signature.parameters.size == args.size }
            .ifEmpty { return null }

        // 1. Look for exact match
        for (candidate in candidates) {
            if (candidate.signature.matchesExactly(args)) {
                return FnMatch.Static(candidate.index, arrayOfNulls(args.size))
            }
        }

        // 2. Discard functions that cannot be matched (via implicit coercion or exact matches)
        val invocableMatches = match(candidates, args).ifEmpty { return null }
        if (invocableMatches.size == 1) {
            return invocableMatches.first().match
        }

        // 3. If there are DYNAMIC nodes, return all candidates
        for (arg in args) {
            if (arg.kind == Kind.DYNAMIC) {
                return FnMatch.Dynamic(invocableMatches.map { it.match })
            }
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

    private class Candidate(
        val index: Int,
        val signature: FnSignature
    )

    /**
     * Resolution of a static function.
     *
     * @param candidates
     * @param args
     * @return
     */
    private fun match(candidates: List<Candidate>, args: List<PType>): List<MatchResult> {
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
    private fun FnSignature.matchesExactly(args: List<PType>): Boolean {
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
    private fun Candidate.match(args: List<PType>): MatchResult? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        var exactInputTypes: Int = 0
        for (i in args.indices) {
            val arg = args[i]
            val p = this.signature.parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> {
                    exactInputTypes++
                    continue
                }
                // 2. Match ANY parameter, no coercion needed
                p.type.kind == Kind.DYNAMIC -> continue
                arg.kind == Kind.UNKNOWN -> continue
                // 3. Allow for ANY arguments
                arg.kind == Kind.DYNAMIC -> {
                    mapping[i] = Ref.Cast(arg, p.type.toCType(), true)
                }
                // 4. Check for a coercion
                else -> when (val coercion = Coercions.get(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion.toPublic()
                }
            }
        }
        return MatchResult(
            FnMatch.Static(this.index, mapping),
            this.signature,
            exactInputTypes,
        )
    }

    private class MatchResult(
        val match: FnMatch.Static,
        val signature: FnSignature,
        val numberOfExactInputTypes: Int,
    )

    private object MatchResultComparator : Comparator<MatchResult> {
        override fun compare(o1: MatchResult, o2: MatchResult): Int {
            return FnComparator.reversed().compare(o1.signature, o2.signature)
        }
    }

    private fun org.partiql.planner.internal.ir.Ref.Cast.toPublic() = Ref.Cast(
        input = input,
        target = target,
        isNullable = isNullable
    )

    /**
     * Result of matching an unresolved function.
     */
    public sealed class FnMatch {

        /**
         * Successful match of a static function call.
         *
         * @property signature
         * @property mapping
         */
        public data class Static(
            val index: Int,
            val mapping: Array<Ref.Cast?>,
        ) : FnMatch() {

            /**
             * The number of exact matches. Useful when ranking function matches.
             */
            val exact: Int = mapping.count { it != null }
        }

        /**
         * This represents dynamic dispatch.
         *
         * @property candidates     Ordered list of potentially applicable functions to dispatch dynamically.
         */
        public data class Dynamic(
            val candidates: List<Static>,
        ) : FnMatch()
    }
}
