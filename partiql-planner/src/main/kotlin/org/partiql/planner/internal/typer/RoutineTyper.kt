package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.casts.Coercions
import org.partiql.planner.internal.fn.FnMatch
import org.partiql.planner.internal.fn.FnResolver
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.refAgg
import org.partiql.planner.internal.ir.refFn
import org.partiql.planner.internal.ir.relOpAggregateCallResolved
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallDynamic
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.metadata.Routine
import org.partiql.types.PType

/**
 * This just pulls out some functions from Env.
 */
internal object RoutineTyper {

    fun typeScalar(variants: List<Routine.Scalar>, args: List<Rex>): Rex? {
        val match = FnResolver.resolve(variants, args.map { it.type })
        // If Type mismatch, then we return a missingOp whose trace is all possible candidates.
        if (match == null) {
            val candidates = variants.map { fnSignature ->
                rexOpCallDynamicCandidate(
                    fn = refFn(
                        catalog = "",
                        path = emptyList(),
                        signature = fnSignature
                    ),
                    coercions = emptyList()
                )
            }
            return ProblemGenerator.missingRex(
                rexOpCallDynamic(args, candidates),
                ProblemGenerator.incompatibleTypesForOp("PLACEHOLDER", args.map { it.type })
            )
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = "",
                            path = emptyList(),
                            signature = it.signature,
                        ),
                        coercions = it.mapping.toList(),
                    )
                }
                // Rewrite as a dynamic call to be typed by PlanTyper
                Rex(CompilerType(PType.typeDynamic()), Rex.Op.Call.Dynamic(args, candidates))
            }
            is FnMatch.Static -> {
                // Create an internal typed reference
                val ref = refFn(
                    catalog = "",
                    path = emptyList(),
                    signature = match.signature,
                )
                // Apply the coercions as explicit casts
                val coercions: List<Rex> = args.mapIndexed { i, arg ->
                    when (val cast = match.mapping[i]) {
                        null -> arg
                        else -> Rex(CompilerType(PType.typeDynamic()), Rex.Op.Cast.Resolved(cast, arg))
                    }
                }
                // Rewrite as a static call to be typed by PlanTyper
                Rex(CompilerType(PType.typeDynamic()), Rex.Op.Call.Static(ref, coercions))
            }
        }
    }

    fun typeAggregation(
        variants: List<Routine.Aggregation>,
        setq: Rel.Op.Aggregate.SetQuantifier,
        args: List<Rex>,
    ): Rel.Op.Aggregate.Call.Resolved? {
        // TODO: Eventually, do we want to support sensitive lookup? With a path?
        val parameters = args.mapIndexed { _, arg -> arg.type }
        val match = match(variants, parameters) ?: return null
        val agg = match.first
        val mapping = match.second
        // Create an internal typed reference
        val ref = refAgg("", emptyList(), agg)
        // Apply the coercions as explicit casts
        val coercions: List<Rex> = args.mapIndexed { i, arg ->
            when (val cast = mapping[i]) {
                null -> arg
                else -> rex(cast.target, rexOpCastResolved(cast, arg))
            }
        }
        return relOpAggregateCallResolved(ref, setq, coercions)
    }

    //-------------------------------------------------------
    // Helpers
    //-------------------------------------------------------

    private fun match(
        candidates: List<Routine.Aggregation>,
        args: List<PType>,
    ): Pair<Routine.Aggregation, Array<Ref.Cast?>>? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return candidate to arrayOfNulls(args.size)
            }
        }
        // 2. Look for best match.
        var match: Pair<Routine.Aggregation, Array<Ref.Cast?>>? = null
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
    private fun Routine.Aggregation.matches(args: List<PType>): Boolean {
        val parameters = getParameters()
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (p.type != PType.Kind.DYNAMIC && a.kind != p.type) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun Routine.Aggregation.match(args: List<PType>): Pair<Routine.Aggregation, Array<Ref.Cast?>>? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        val parameters = getParameters()
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg.kind == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type == PType.Kind.DYNAMIC -> continue
                // 3. Check for a coercion
                else -> when (val coercion = Coercions.get(arg, PType.fromKind(p.type))) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return this to mapping
    }
}
