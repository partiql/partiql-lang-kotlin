package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Catalogs
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.casts.Coercions
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.SetQuantifier
import org.partiql.planner.internal.ir.refAgg
import org.partiql.planner.internal.ir.refFn
import org.partiql.planner.internal.ir.relOpAggregateCallResolved
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallDynamic
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.ir.rexOpVarGlobal
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.Scope.Companion.toPath
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.SqlFnProvider
import org.partiql.types.PType
import org.partiql.types.PType.Kind

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

    private val catalogs: Catalogs = session.getCatalogs()

    /**
     * Current [Catalog] implementation; error if missing from the [Catalogs] provider.
     */
    private val default: Catalog = catalogs.getCatalog(session.getCatalog()) ?: error("Default catalog does not exist")

    /**
     * A [SqlFnProvider] for looking up built-in functions.
     */
    private val fns: SqlFnProvider = SqlFnProvider

    /**
     * Catalog lookup needs to search (3x) to handle schema-qualified and catalog-qualified use-cases.
     *
     *  1. Lookup in current catalog and namespace.
     *  2. Lookup as a schema-qualified identifier.
     *  3. Lookup as a catalog-qualified identifier.
     *
     */
    fun resolveTable(identifier: Identifier): Rex? {

        // 1. Search in current catalog and namespace
        var catalog = default
        var path = resolve(identifier)
        var handle = catalog.getTableHandle(session, path)

        // 2. Lookup as a schema-qualified identifier.
        if (handle == null && identifier.hasQualifier()) {
            path = identifier
            handle = catalog.getTableHandle(session, path)
        }

        // 3. Lookup as a catalog-qualified identifier
        if (handle == null && identifier.hasQualifier()) {
            val parts = identifier.getParts()
            val head = parts.first()
            val tail = parts.drop(1)
            catalog = catalogs.getCatalog(head.getText(), ignoreCase = head.isRegular()) ?: return null
            path = Identifier.of(tail)
            handle = catalog.getTableHandle(session, path)
        }

        // !! NOT FOUND !!
        if (handle == null) {
            return null
        }

        // Make a reference and return a global variable expression.
        val refCatalog = catalog.getName()
        val refName = handle.name
        val refType = CompilerType(handle.table.getSchema())
        val ref = Ref.Obj(refCatalog, refName, refType)

        // Convert any remaining identifier parts to a path expression
        val root = Rex(ref.type, rexOpVarGlobal(ref))
        val tail = calculateMatched(path, handle.name)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

    fun resolveFn(identifier: Identifier, args: List<Rex>): Rex? {
        // Assume all functions are defined in the current catalog and reject qualified routine names.
        if (identifier.hasQualifier()) {
            error("Qualified functions are not supported.")
        }
        val catalog = session.getCatalog()
        val name = identifier.getIdentifier().getText().lowercase()
        // Invoke existing function resolution logic
        val variants = fns.lookupFn(name) ?: return null
        val match = FnResolver.resolve(variants, args.map { it.type })
        // If Type mismatch, then we return a missingOp whose trace is all possible candidates.
        if (match == null) {
            val candidates = variants.map { fnSignature ->
                rexOpCallDynamicCandidate(
                    fn = refFn(
                        catalog = catalog,
                        name = Name.of(name),
                        signature = fnSignature
                    ),
                    coercions = emptyList()
                )
            }
            // TODO consistency for error messages?
            return ProblemGenerator.missingRex(
                rexOpCallDynamic(args, candidates),
                ProblemGenerator.incompatibleTypesForOp(name.uppercase(), args.map { it.type })
            )
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = catalog,
                            name = Name.of(name),
                            signature = it.signature,
                        ),
                        coercions = it.mapping.toList(),
                    )
                }
                // Rewrite as a dynamic call to be typed by PlanTyper
                Rex(CompilerType(PType.dynamic()), Rex.Op.Call.Dynamic(args, candidates))
            }
            is FnMatch.Static -> {
                // Create an internal typed reference
                val ref = refFn(
                    catalog = catalog,
                    name = Name.of(name),
                    signature = match.signature,
                )
                // Apply the coercions as explicit casts
                val coercions: List<Rex> = args.mapIndexed { i, arg ->
                    when (val cast = match.mapping[i]) {
                        null -> arg
                        else -> Rex(CompilerType(PType.dynamic()), Rex.Op.Cast.Resolved(cast, arg))
                    }
                }
                // Rewrite as a static call to be typed by PlanTyper
                Rex(CompilerType(PType.dynamic()), Rex.Op.Call.Static(ref, coercions))
            }
        }
    }

    fun resolveAgg(path: String, setQuantifier: SetQuantifier, args: List<Rex>): Rel.Op.Aggregate.Call.Resolved? {
        // TODO: Eventually, do we want to support sensitive lookup? With a path?
        val catalog = session.getCatalog()
        val name = path.lowercase()
        // Invoke existing function resolution logic
        val candidates = fns.lookupAgg(name) ?: return null
        val parameters = args.mapIndexed { i, arg -> arg.type }
        val match = match(candidates, parameters) ?: return null
        val agg = match.first
        val mapping = match.second
        // Create an internal typed reference
        val ref = refAgg(catalog, Name.of(name), agg)
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

    private fun match(candidates: List<AggSignature>, args: List<PType>): Pair<AggSignature, Array<Ref.Cast?>>? {
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

    private fun AggSignature.matches(args: List<PType>): Boolean {
        for (i in args.indices) {
            val a = args[i]
            val p = parameters[i]
            if (p.type.kind != Kind.DYNAMIC && a != p.type) return false
        }
        return true
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     *
     * @param args
     * @return
     */
    private fun AggSignature.match(args: List<PType>): Pair<AggSignature, Array<Ref.Cast?>>? {
        val mapping = arrayOfNulls<Ref.Cast?>(args.size)
        for (i in args.indices) {
            val arg = args[i]
            val p = parameters[i]
            when {
                // 1. Exact match
                arg == p.type -> continue
                // 2. Match ANY, no coercion needed
                p.type.kind == Kind.DYNAMIC -> continue
                // 3. Check for a coercion
                else -> when (val coercion = Coercions.get(arg, p.type)) {
                    null -> return null // short-circuit
                    else -> mapping[i] = coercion
                }
            }
        }
        return this to mapping
    }
}
