package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Catalogs
import org.partiql.planner.catalog.Function
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.functions.FnMatch
import org.partiql.planner.internal.functions.FnResolver
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.refFn
import org.partiql.planner.internal.ir.rexOpCallDynamic
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.ir.rexOpVarGlobal
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath
import org.partiql.types.PType

/**
 * [Env] is similar to the database type environment from the PartiQL Specification. This includes resolution of
 * database binding values and scoped functions.
 *
 * See TypeEnv for the variables type environment.
 */
internal class Env(
    private val catalogs: Catalogs,
    private val session: Session,
) {

    /**
     * Current [Catalog] implementation; error if missing from the [Catalogs] provider.
     */
    private val default: Catalog = catalogs.get(session.getCatalog()) ?: error("Default catalog does not exist")

    /**
     * Catalog lookup...
     */
    fun getTable(identifier: Identifier): Rex? {
        // lookup at current catalog and current namespace
        var catalog = default
        val path = resolve(identifier)
        var handle = catalog.getTableHandle(session, path)
        if (handle == null && identifier.hasQualifier()) {
            // lookup to see if qualifier
            val head = identifier.first()
            val tail = Identifier.of(identifier.drop(1))
            catalog = catalogs.get(head.getText(), ignoreCase = head.isRegular()) ?: return null
            handle = catalog.getTableHandle(session, tail)
        }
        // NOT FOUND!
        if (handle == null) {
            return null
        }
        // Make a reference and return a global variable expression.
        val refCatalog = catalog.getName()
        val refName = handle.name
        val refType = CompilerType(handle.table.getSchema())
        val ref = Ref.Table(refCatalog, refName, refType)

        // Convert any remaining identifier parts to a path expression
        val root = Rex(ref.type, rexOpVarGlobal(ref))
        val tail = calculateMatched(path, handle.name)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

    /**
     * TODO use session PATH.
     */
    fun getFunction(identifier: Identifier, args: List<Rex>): Rex? {
        // don't allow qualified function invocations for the current version .
        if (identifier.hasQualifier()) {
            error("Function resolution with qualifier not supported")
        }
        // case-normalize lower routine names
        val catalog = default
        val name = Name.of(identifier.map { it.getText().lowercase() })
        val variants = catalog.getFunctions(session, name)
            .filterIsInstance<Function.Scalar>()
            .toList()
        // function not found
        if (variants.isEmpty()) {
            return null
        }
        // attempt to match
        val match = FnResolver.resolve(variants, args.map { it.type })
        // If Type mismatch, then we return a missingOp whose trace is all possible candidates.
        if (match == null) {
            val candidates = variants.map { fnSignature ->
                rexOpCallDynamicCandidate(
                    fn = refFn(
                        catalog = catalog.getName(),
                        name = name,
                        signature = fnSignature
                    ),
                    coercions = emptyList()
                )
            }
            return ProblemGenerator.missingRex(
                rexOpCallDynamic(args, candidates),
                ProblemGenerator.incompatibleTypesForOp(name.toString(), args.map { it.type })
            )
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                val candidates = match.candidates.map {
                    // Create an internal typed reference for every candidate
                    rexOpCallDynamicCandidate(
                        fn = refFn(
                            catalog = catalog.getName(),
                            name = name,
                            signature = it.signature as Function.Scalar,
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
                    catalog = catalog.getName(),
                    name = name,
                    signature = match.signature as Function.Scalar,
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

    fun resolveCast(input: Rex, target: CompilerType): Rex.Op.Cast.Resolved? {
        val operand = input.type
        val cast = CastTable.partiql.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }

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
        return rhs.take(rhs.size - lhs.size)
    }
}
