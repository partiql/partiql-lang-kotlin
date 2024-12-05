package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.SetQuantifier
import org.partiql.planner.internal.ir.refAgg
import org.partiql.planner.internal.ir.refFn
import org.partiql.planner.internal.ir.relOpAggregateCallResolved
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.ir.rexOpVarGlobal
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.internal.typer.Scope.Companion.toPath
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Catalogs
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Function
import org.partiql.types.PType

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
internal class Env(private val session: Session) {

    /**
     * Catalogs provider.
     */
    private val catalogs: Catalogs = session.getCatalogs()

    /**
     * Current [Catalog] implementation; error if missing from the [Catalogs] provider.
     */
    private val default: Catalog = catalogs.getCatalog(session.getCatalog()) ?: error("Default catalog does not exist")

    /**
     * Catalog lookup needs to search (3x) to table schema-qualified and catalog-qualified use-cases.
     *
     *  1. Lookup in current catalog and namespace.
     *  2. Lookup as a schema-qualified identifier.
     *  3. Lookup as a catalog-qualified identifier.
     */
    fun resolveTable(identifier: Identifier): Rex? {

        // 1. Search in current catalog and namespace
        var catalog = default
        var path = resolve(identifier)
        var table = catalog.getTable(session, path)

        // 2. Lookup as a schema-qualified identifier.
        if (table == null && identifier.hasQualifier()) {
            path = identifier
            table = catalog.getTable(session, path)
        }

        // 3. Lookup as a catalog-qualified identifier
        if (table == null && identifier.hasQualifier()) {
            val parts = identifier.getParts()
            val head = parts.first()
            val tail = parts.drop(1)
            catalog = catalogs.getCatalog(head.getText(), ignoreCase = head.isRegular()) ?: return null
            path = Identifier.of(tail)
            table = catalog.getTable(session, path)
        }

        // !! NOT FOUND !!
        if (table == null) {
            return null
        }

        // Make a reference and return a global variable expression.
        val refCatalog = catalog.getName()
        val refName = table.getName()
        val refType = CompilerType(table.getSchema())
        val ref = Ref.Obj(refCatalog, refName, refType, table)

        // Convert any remaining identifier parts to a path expression
        val root = Rex(ref.type, rexOpVarGlobal(ref))
        val tail = calculateMatched(path, refName)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

    /**
     * @return a list of candidate functions that match the [identifier] and number of [args].
     */
    fun getCandidates(identifier: Identifier, args: List<Rex>): List<Function> {
        // Reject qualified routine names.
        if (identifier.hasQualifier()) {
            error("Qualified functions are not supported.")
        }

        // 1. Search in the current catalog and namespace.
        val catalog = default
        val name = identifier.getIdentifier().getText().lowercase() // CASE-NORMALIZED LOWER
        val variants = catalog.getFunctions(session, name).toList()
        val candidates = variants.filter { it.getParameters().size == args.size }
        return candidates
    }

    /**
     * TODO leverage session PATH.
     *
     * @param identifier
     * @param args
     * @return
     */
    fun resolveFn(identifier: Identifier, args: List<Rex>): Rex? {

        // Reject qualified routine names.
        if (identifier.hasQualifier()) {
            error("Qualified functions are not supported.")
        }

        // 1. Search in the current catalog and namespace.
        val catalog = default
        val name = identifier.getIdentifier().getText().lowercase() // CASE-NORMALIZED LOWER
        val variants = catalog.getFunctions(session, name).toList()
        if (variants.isEmpty()) {
            return null
        }

        // 2. Search along the PATH.
        // TODO
        val match = FnResolver.resolve(variants, args.map { it.type })
        // If Type mismatch, then we return a missingOp whose trace is all possible candidates.
        if (match == null) {
            return null
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = catalog.getName(),
                            name = Name.of(name),
                            signature = it,
                        ),
                        coercions = emptyList(), // TODO: Remove this from the plan
                    )
                }
                // Rewrite as a dynamic call to be typed by PlanTyper
                Rex(CompilerType(PType.dynamic()), Rex.Op.Call.Dynamic(args, candidates))
            }
            is FnMatch.Static -> {
                // Apply the coercions as explicit casts
                val coercions: List<Rex> = args.mapIndexed { i, arg ->
                    when (val cast = match.mapping[i]) {
                        null -> arg
                        else -> Rex(cast.target, Rex.Op.Cast.Resolved(cast, arg))
                    }
                }
                // Rewrite as a static call to be typed by PlanTyper
                Rex(CompilerType(PType.dynamic()), Rex.Op.Call.Static(match.function, coercions))
            }
        }
    }

    fun resolveAgg(path: String, setQuantifier: SetQuantifier, args: List<Rex>): Rel.Op.Aggregate.Call.Resolved? {
        // TODO: Eventually, do we want to support sensitive lookup? With a path?

        // 1. Search in the current catalog and namespace.
        val catalog = default
        val name = path.lowercase()
        val candidates = catalog.getAggregations(session, name).toList()
        if (candidates.isEmpty()) {
            return null
        }

        // 2. Search along the PATH.
        // TODO

        // Invoke existing function resolution logic
        val parameters = args.map { it.type }
        val match = match(candidates, parameters) ?: return null
        val agg = match.first
        val mapping = match.second
        // Create an internal typed reference
        val ref = refAgg(catalog.getName(), Name.of(name), agg)
        // Apply the coercions as explicit casts
        val coercions: List<Rex> = args.mapIndexed { i, arg ->
            when (val cast = mapping[i]) {
                null -> arg
                else -> rex(cast.target, rexOpCastResolved(cast, arg))
            }
        }
        return relOpAggregateCallResolved(ref, setQuantifier, coercions)
    }

    fun resolveCast(input: Rex, target: CompilerType): Rex.Op.Cast.Resolved? {
        val operand = input.type
        val cast = CastTable.partiql.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }

    // -----------------------
    //  Helpers
    // -----------------------

    // Helpers

    /**
     * Prepends the current session namespace to the identifier; named like Path.resolve() from java io.
     */
    private fun resolve(identifier: Identifier): Identifier {
        val namespace = session.getNamespace()
        return if (namespace.isEmpty()) {
            // no need to create another object
            identifier
        } else {
            // prepend the namespace
            namespace.asIdentifier().append(identifier)
        }
    }

    /**
     * Returns a list of the unmatched parts of the identifier given the matched name.
     */
    private fun calculateMatched(path: Identifier, name: Name): List<Identifier.Part> {
        val lhs = name.toList()
        val rhs = path.toList()
        return rhs.takeLast(rhs.size - lhs.size)
    }

    private fun match(candidates: List<Aggregation>, args: List<PType>): Pair<Aggregation, Array<Ref.Cast?>>? {
        // 1. Check for an exact match
        for (candidate in candidates) {
            if (candidate.matches(args)) {
                return candidate to arrayOfNulls(args.size)
            }
        }
        // 2. Look for best match.
        var match: Pair<Aggregation, Array<Ref.Cast?>>? = null
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
    private fun Aggregation.matches(args: List<PType>): Boolean {
        val parameters = getParameters()
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (p.getMatch(a) != a) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun Aggregation.match(args: List<PType>): Pair<Aggregation, Array<Ref.Cast?>>? {
        val parameters = getParameters()
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            val m = p.getMatch(a)
            when {
                m == null -> return null
                m == a -> continue
                else -> mapping[i] = coercion(a, m)
            }
        }
        return this to mapping
    }

    private fun coercion(arg: PType, target: PType): Ref.Cast {
        return Ref.Cast(arg.toCType(), target.toCType(), Ref.Cast.Safety.COERCION, true)
    }
}
