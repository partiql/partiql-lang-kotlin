package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.casts.CastTable
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
import org.partiql.planner.internal.ir.rexOpGlobal
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath
import org.partiql.planner.internal.typer.toRuntimeType
import org.partiql.planner.internal.typer.toStaticType
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
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
    private val aggs: PathResolverAgg = PathResolverAgg

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
            path = item.handle.path,
            type = item.handle.entity.getType(),
        )
        // Rewrite as a path expression.
        val root = rex(ref.type, rexOpGlobal(ref))
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
        if (match == null) {
            // unable to make a match, consider returning helpful error messages given the item.variants.
            return null
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = item.catalog,
                            path = item.handle.path,
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
                    path = item.handle.path,
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
    fun resolveAgg(name: String, args: List<Rex>): Rel.Op.Aggregate.Call.Resolved? {
        val match = aggs.resolve(name, args) ?: return null
        val agg = match.first
        val mapping = match.second
        // Create an internal typed reference
        val ref = refAgg(name, agg)
        // Apply the coercions as explicit casts
        val coercions: List<Rex> = args.mapIndexed { i, arg ->
            when (val cast = mapping[i]) {
                null -> arg
                else -> rex(cast.target.toStaticType(), rexOpCastResolved(cast, arg))
            }
        }
        return relOpAggregateCallResolved(ref, coercions)
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
     * 1. Matched = RelativePath - Not Found
     * 2. Not Found = Input CatalogPath - Output CatalogPath
     * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
     * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
     */
    private fun calculateMatched(
        originalPath: BindingPath,
        inputCatalogPath: BindingPath,
        outputCatalogPath: List<String>,
    ): Int {
        return originalPath.steps.size + outputCatalogPath.size - inputCatalogPath.steps.size
    }
}
