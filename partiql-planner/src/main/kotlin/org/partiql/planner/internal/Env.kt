package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.catalogItemFn
import org.partiql.planner.internal.ir.catalogItemValue
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallStatic
import org.partiql.planner.internal.ir.rexOpGlobal
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath
import org.partiql.planner.internal.typer.toRuntimeType
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

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
     * Maintain a list of all resolved catalog symbols (objects and functions).
     */
    private val symbols: Symbols = Symbols.empty()

    /**
     * Convert the symbols structure into a list of [Catalog] for shipping in the plan.
     */
    internal fun catalogs(): List<Catalog> = symbols.build()

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
    private val functions: PathResolverFn = PathResolverFn(catalog, session)

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
        // Insert into symbols, producing a reference
        val symbol = catalogItemValue(
            path = item.handle.path,
            type = item.handle.entity.getType(),
        )
        val ref = symbols.insert(item.catalog, symbol)
        // Rewrite as a path expression.
        val root = rex(symbol.type, rexOpGlobal(ref))
        val depth = calculateMatched(path, item.input, symbol.path)
        val tail = path.steps.drop(depth)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

    @OptIn(FnExperimental::class)
    fun resolveFn(path: BindingPath, args: List<Rex>): Rex? {
        val item = functions.lookup(path) ?: return null
        // Invoke FnResolver to determine if we made a match
        val variants = item.handle.entity.getVariants()
        val match = FnResolver.resolve(variants, args.map { it.type })
        if (match == null) {
            // unable to make a match, consider returning helpful error messages given the item.variants.
            return null
        }
        return when (match) {
            is FnMatch.Dynamic -> {
                TODO()
            }
            is FnMatch.Static -> {
                // Insert into symbols, producing a reference
                val symbol = catalogItemFn(
                    path = item.handle.path,
                    specific = match.signature.specific,
                )
                val ref = symbols.insert(item.catalog, symbol)
                // Rewrite as a static call to by typed by PlanTyper
                rex(StaticType.ANY, rexOpCallStatic(ref, args))
            }
        }
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
