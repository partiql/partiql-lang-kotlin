package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
import org.partiql.spi.BindingPath

/**
 * [Env] represents the combination of the database type environment (db env) and some local env (type env).
 *
 * @property session
 */
internal class Env(private val session: PartiQLPlanner.Session) {

    /**
     * Collect the list of all referenced globals during planning.
     */
    public val catalogs = mutableListOf<Catalog>()

    /**
     * Catalog Metadata for this query session.
     */
    private val connectors = session.catalogs

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] scalar function catalog.
     */
    internal fun resolveFn(fn: Fn.Unresolved, args: List<Rex>) = fnResolver.resolveFn(fn, args)

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] aggregation function catalog.
     */
    internal fun resolveAgg(agg: Agg.Unresolved, args: List<Rex>) = fnResolver.resolveAgg(agg, args)

    /**
     * Fetch global object metadata from the given [BindingPath].
     *
     * @param catalog   Current catalog
     * @param path      Global identifier path
     * @return
     */
    fun catalogs(): List<Catalog> = catalogs.map { it.build() }

    /**
     *
     *
     * @param path
     * @param locals
     * @param strategy
     * @return
     */
    public fun resolve(path: BindingPath, locals: TypeEnv, strategy: ResolutionStrategy): Rex? = when (strategy) {
        ResolutionStrategy.LOCAL -> locals.resolve(path) ?: catalog.lookup
        ResolutionStrategy.GLOBAL -> global(path) ?: locals.resolve(path)
    }


    /**
     * TODO optimization, check known globals before calling out to connector again
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getGlobalType(
        catalog: BindingName?,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ): ResolvedVar? {
        return catalog?.let { cat ->
            getObjectHandle(cat, catalogPath)?.let { handle ->
                getObjectDescriptor(handle).let { type ->
                    val depth = calculateMatched(originalPath, catalogPath, handle.second.absolutePath)
                    val (catalogIndex, valueIndex) = getOrAddCatalogValue(
                        handle.first,
                        handle.second.absolutePath.steps,
                        type
                    )
                    // Return resolution metadata
                    ResolvedVar(type, catalogIndex, depth, valueIndex)
                }
            }
        }
    }


    // /**
    //  * TODO
    //  *
    //  * @param fn
    //  * @return
    //  */
    // @OptIn(FnExperimental::class)
    // public fun resolve(fn: Fn.Unresolved, args: List<Rex>): Rex? {
    //     TODO()
    // }

    /**
     * Attempt to resolve a [BindingPath] in the global + local type environments.
     */
    fun resolve(path: BindingPath, locals: TypeEnv, strategy: ResolutionStrategy): Rex? {
        return when (strategy) {
            ResolutionStrategy.LOCAL -> locals.resolve(path) ?: resolveGlobalBind(path)
            ResolutionStrategy.GLOBAL -> resolveGlobalBind(path) ?: locals.resolve(path)
        }
    }

    /**
     * Logic is as follows:
     * 1. If Current Catalog and Schema are set, create a Path to the object and attempt to grab handle and schema.
     *   a. If not found, just try to find the object in the catalog.
     * 2. If Current Catalog is not set:
     *   a. Loop through all catalogs and try to find the object.
     *
     * TODO: Add global bindings
     * TODO: Replace paths with global variable references if found
     */
    private fun resolveGlobalBind(path: BindingPath): Rex? {
        val currentCatalog = session.currentCatalog?.let { BindingName(it, BindingCase.SENSITIVE) }
        val currentCatalogPath = BindingPath(session.currentDirectory.map { BindingName(it, BindingCase.SENSITIVE) })
        val absoluteCatalogPath = BindingPath(currentCatalogPath.steps + path.steps)
        val resolvedVar = when (path.steps.size) {
            0 -> null
            1 -> getGlobalType(currentCatalog, path, absoluteCatalogPath)
            2 -> getGlobalType(currentCatalog, path, path) ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
            else -> {
                val inferredCatalog = path.steps[0]
                val newPath = BindingPath(path.steps.subList(1, path.steps.size))
                getGlobalType(inferredCatalog, path, newPath)
                    ?: getGlobalType(currentCatalog, path, path)
                    ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
            }
        } ?: return null
        // rewrite as path expression for any remaining steps.
        val root = rex(resolvedVar.type, rexOpGlobal(catalogSymbolRef(resolvedVar.ordinal, resolvedVar.position)))
        val tail = path.steps.drop(resolvedVar.depth)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }

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
        outputCatalogPath: ConnectorObjectPath,
    ): Int {
        return originalPath.steps.size + outputCatalogPath.steps.size - inputCatalogPath.steps.size
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.toPath(steps: List<BindingName>): Rex = steps.fold(this) { curr, step ->
        val op = when (step.bindingCase) {
            BindingCase.SENSITIVE -> rexOpPathKey(curr, rex(StaticType.STRING, rexOpLit(stringValue(step.name))))
            BindingCase.INSENSITIVE -> rexOpPathSymbol(curr, step.name)
        }
        rex(StaticType.ANY, op)
    }
}