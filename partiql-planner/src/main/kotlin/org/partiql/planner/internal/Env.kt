package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.refAgg
import org.partiql.planner.internal.ir.refFn
import org.partiql.planner.internal.ir.refObj
import org.partiql.planner.internal.ir.relOpAggregateCallResolved
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallDynamic
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCallStatic
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.ir.rexOpVarGlobal
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath
import org.partiql.planner.internal.typer.toRuntimeType
import org.partiql.planner.internal.typer.toStaticType
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * [Env] is similar to the database type environment from the PartiQL Specification. This includes resolution of
 * database binding values and scoped functions.
 *
 * See TypeEnv for the variables type environment.
 *
 * TODO: function resolution between scalar functions and aggregations.
 *
 * @property session
 */
internal class Env(private val session: PartiQLPlanner.Session) {

    /**
     * Cast table used for coercion and explicit cast resolution.
     */
    private val casts = CastTable.partiql

    /**
     * Current catalog [ConnectorMetadata]. Error if missing from the session.
     */
    private val catalog: ConnectorMetadata = session.catalogs[session.currentCatalog]
        ?: error("Session is missing ConnectorMetadata for current catalog ${session.currentCatalog}")

    /**
     * A [PathResolver] for looking up objects given both unqualified and qualified names.
     */
    private val objects: PathResolverObj = PathResolverObj(catalog, session)

    /**
     * A [PathResolver] for looking up functions given both unqualified and qualified names.
     */
    private val fns: PathResolverFn = PathResolverFn(catalog, session)

    /**
     * A [PathResolver] for aggregation function lookup.
     */
    private val aggs: PathResolverAgg = PathResolverAgg(catalog, session)

    /**
     * This function looks up a global [BindingPath], returning a global reference expression.
     *
     * Convert any remaining binding names (tail) to a path expression.
     *
     * @param path
     * @return
     */
    fun resolveObj(path: BindingPath): Rex? {
        val item = objects.lookup(path) ?: return null
        // Create an internal typed reference
        val ref = refObj(
            catalog = item.catalog,
            path = item.handle.path.steps,
            type = item.handle.entity.getType(),
        )
        // Rewrite as a path expression.
        val root = rex(ref.type, rexOpVarGlobal(ref))
        val depth = calculateMatched(path, item.input, ref.path)
        val tail = path.steps.drop(depth)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    fun resolveFn(path: BindingPath, args: List<Rex>): Rex? {
        val item = fns.lookup(path) ?: return null
        // Invoke FnResolver to determine if we made a match
        val variants = item.handle.entity.getVariants()
        val match = FnResolver.resolve(variants, args.map { it.type })
        // If Type mismatch, then we return a missingOp whose trace is all possible candidates.
        if (match == null) {
            val candidates = variants.map { fnSignature ->
                rexOpCallDynamicCandidate(
                    fn = refFn(
                        item.catalog,
                        path = item.handle.path.steps,
                        signature = fnSignature
                    ),
                    coercions = emptyList()
                )
            }
            return ProblemGenerator.missingRex(
                rexOpCallDynamic(args, candidates, false),
                ProblemGenerator.incompatibleTypesForOp(args.map { it.type }, path.normalized.joinToString("."))
            )
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = item.catalog,
                            path = item.handle.path.steps,
                            signature = it.signature,
                        ),
                        coercions = it.mapping.toList(),
                    )
                }
                // Rewrite as a dynamic call to be typed by PlanTyper
                rex(StaticType.ANY, rexOpCallDynamic(args, candidates, match.exhaustive))
            }
            is FnMatch.Static -> {
                // Create an internal typed reference
                val ref = refFn(
                    catalog = item.catalog,
                    path = item.handle.path.steps,
                    signature = match.signature,
                )
                // Apply the coercions as explicit casts
                val coercions: List<Rex> = args.mapIndexed { i, arg ->
                    when (val cast = match.mapping[i]) {
                        null -> arg
                        else -> rex(StaticType.ANY, rexOpCastResolved(cast, arg))
                    }
                }
                // Rewrite as a static call to be typed by PlanTyper
                rex(StaticType.ANY, rexOpCallStatic(ref, coercions))
            }
        }
    }

    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    fun resolveAgg(name: String, setQuantifier: Rel.Op.Aggregate.SetQuantifier, args: List<Rex>): Rel.Op.Aggregate.Call.Resolved? {
        // TODO: Eventually, do we want to support sensitive lookup? With a path?
        val path = BindingPath(listOf(BindingName(name, BindingCase.INSENSITIVE)))
        val item = aggs.lookup(path) ?: return null
        val candidates = item.handle.entity.getVariants()
        var hadMissingArg = false
        val parameters = args.mapIndexed { i, arg ->
            if (!hadMissingArg && arg.type.isMissable()) {
                hadMissingArg = true
            }
            arg.type.toRuntimeType()
        }
        val match = match(candidates, parameters) ?: return null
        val agg = match.first
        val mapping = match.second
        // Create an internal typed reference
        val ref = refAgg(item.catalog, item.handle.path.steps, agg)
        // Apply the coercions as explicit casts
        val coercions: List<Rex> = args.mapIndexed { i, arg ->
            when (val cast = mapping[i]) {
                null -> arg
                else -> rex(cast.target.toStaticType(), rexOpCastResolved(cast, arg))
            }
        }
        return relOpAggregateCallResolved(ref, setQuantifier, coercions)
    }

    @OptIn(PartiQLValueExperimental::class)
    fun resolveCast(input: Rex, target: PartiQLValueType): Rex.Op.Cast.Resolved? {
        val operand = input.type.toRuntimeType()
        val cast = casts.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }

    // -----------------------
    //  Helpers
    // -----------------------

    /**
     * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
     *
     * Assume:
     * - steps_matched = user_input_path_size - path_steps_not_found_size
     * - path_steps_not_found_size = catalog_path_sent_to_spi_size - actual_catalog_absolute_path_size
     *
     * Therefore, we present the equation to [calculateMatched]:
     * - steps_matched = user_input_path_size - (catalog_path_sent_to_spi_size - actual_catalog_absolute_path_size)
     *                 = user_input_path_size + actual_catalog_absolute_path_size - catalog_path_sent_to_spi_size
     *
     * For example:
     *
     * Assume we are in some catalog, C, in some schema, S. There is a tuple, T, with attribute, A1. Assume A1 is of type
     * tuple with an attribute A2.
     * If our query references `T.A1.A2`, we will eventually ask SPI (connector C) for `S.T.A1.A2`. In this scenario:
     * - The original user input was `T.A1.A2` (length 3)
     * - The absolute path returned from SPI will be `S.T` (length 2)
     * - The path we eventually sent to SPI to resolve was `S.T.A1.A2` (length 4)
     *
     * So, we can now use [calculateMatched] to determine how many were actually matched from the user input. Using the
     * equation from above:
     *
     * - steps_matched = len(user input) + len(absolute catalog path) - len(path sent to SPI)
     * = len([userInputPath]) + len([actualAbsolutePath]) - len([pathSentToConnector])
     * = 3 + 2 - 4
     * = 5 - 4
     * = 1
     *
     *
     * Therefore, in this example we have determined that from the original input (`T.A1.A2`) `T` is the value matched in the
     * database environment.
     */
    private fun calculateMatched(
        userInputPath: BindingPath,
        pathSentToConnector: BindingPath,
        actualAbsolutePath: List<String>,
    ): Int {
        return userInputPath.steps.size + actualAbsolutePath.size - pathSentToConnector.steps.size
    }

    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    private fun match(candidates: List<AggSignature>, args: List<PartiQLValueType>): Pair<AggSignature, Array<Ref.Cast?>>? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return candidate to arrayOfNulls(args.size)
            }
        }
        // 2. Look for best match.
        var match: Pair<AggSignature, Array<Ref.Cast?>>? = null
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
    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    private fun AggSignature.matches(args: List<PartiQLValueType>): Boolean {
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (p.type != PartiQLValueType.ANY && a != p.type) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    private fun AggSignature.match(args: List<PartiQLValueType>): Pair<AggSignature, Array<Ref.Cast?>>? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type == PartiQLValueType.ANY -> continue
                // 3. Match NULL argument
                arg == PartiQLValueType.NULL -> continue
                // 4. Check for a coercion
                else -> when (val coercion = PathResolverAgg.casts.lookupCoercion(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return this to mapping
    }
}
