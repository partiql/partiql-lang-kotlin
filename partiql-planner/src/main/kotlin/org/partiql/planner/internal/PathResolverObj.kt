package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ResolvedVar
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObject

internal class PathResolverObj(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<ConnectorObject>(catalog, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle<ConnectorObject>? {
        TODO("Not yet implemented")
    }

    /**
     * Lookup a global using a [BindingName] as the catalog name.
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getGlobalType(
        catalog: BindingName,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ): ResolvedVar? {
        val cat = catalogs.keys.firstOrNull { catalog.matches(it) } ?: return null
        return getGlobalType(cat, originalPath, catalogPath)
    }
    //
    // /**
    //  * TODO optimization, check known globals before calling out to connector again
    //  *
    //  * @param catalog
    //  * @param originalPath
    //  * @param catalogPath
    //  * @return
    //  */
    // private fun getGlobalType(
    //     catalog: String,
    //     originalPath: BindingPath,
    //     catalogPath: BindingPath,
    // ): ResolvedVar? {
    //     return catalogs[catalog]?.let { metadata ->
    //         metadata.getObject(catalogPath)?.let { obj ->
    //             obj.entity.getType()?.let { type ->
    //                 val depth = calculateMatched(originalPath, catalogPath, obj.path)
    //                 val (catalogIndex, valueIndex) = getOrAddCatalogValue(catalog, obj.path, type)
    //                 // Return resolution metadata
    //                 ResolvedVar.Global(type, catalogIndex, depth, valueIndex)
    //             }
    //         }
    //     }
    // }
    //
    // /**
    //  * @return a [Pair] where [Pair.first] is the catalog index and [Pair.second] is the value index within that catalog
    //  */
    // private fun getOrAddCatalogValue(
    //     catalogName: String,
    //     valuePath: List<String>,
    //     valueType: StaticType,
    // ): Pair<Int, Int> {
    //     val catalogIndex = getOrAddCatalog(catalogName)
    //     val symbols = symbols[catalogIndex].symbols
    //     return symbols.indexOfFirst { value ->
    //         value.path == valuePath
    //     }.let { index ->
    //         when (index) {
    //             -1 -> {
    //                 this.symbols[catalogIndex] = this.symbols[catalogIndex].copy(
    //                     symbols = symbols + listOf(Catalog.Symbol(valuePath, valueType))
    //                 )
    //                 catalogIndex to this.symbols[catalogIndex].symbols.lastIndex
    //             }
    //             else -> {
    //                 catalogIndex to index
    //             }
    //         }
    //     }
    // }
    //
    // private fun getOrAddCatalog(catalogName: String): Int {
    //     return symbols.indexOfFirst { catalog ->
    //         catalog.name == catalogName
    //     }.let {
    //         when (it) {
    //             -1 -> {
    //                 symbols.add(Catalog(catalogName, emptyList()))
    //                 symbols.lastIndex
    //             }
    //             else -> it
    //         }
    //     }
    // }
    //
    // /**
    //  * Attempt to resolve a [BindingPath] in the global + local type environments.
    //  */
    // fun resolve(path: BindingPath, locals: TypeEnv, scope: Rex.Op.Var.Scope): ResolvedVar? {
    //     val strategy = when (scope) {
    //         Rex.Op.Var.Scope.DEFAULT -> locals.strategy
    //         Rex.Op.Var.Scope.LOCAL -> ResolutionStrategy.LOCAL
    //     }
    //     return when (strategy) {
    //         ResolutionStrategy.LOCAL -> {
    //             var type: ResolvedVar? = null
    //             type = type ?: resolveLocalBind(path, locals.schema)
    //             type = type ?: resolveGlobalBind(path)
    //             type
    //         }
    //         ResolutionStrategy.GLOBAL -> {
    //             var type: ResolvedVar? = null
    //             type = type ?: resolveGlobalBind(path)
    //             type = type ?: resolveLocalBind(path, locals.schema)
    //             type
    //         }
    //     }
    // }
    //
    // /**
    //  * Logic is as follows:
    //  * 1. If Current Catalog and Schema are set, create a Path to the object and attempt to grab handle and schema.
    //  *   a. If not found, just try to find the object in the catalog.
    //  * 2. If Current Catalog is not set:
    //  *   a. Loop through all catalogs and try to find the object.
    //  *
    //  * TODO: Replace paths with global variable references if found
    //  */
    // private fun resolveGlobalBind(path: BindingPath): ResolvedVar? {
    //     val currentCatalog = session.currentCatalog
    //     val currentCatalogPath = BindingPath(session.currentDirectory.map { BindingName(it, BindingCase.SENSITIVE) })
    //     val absoluteCatalogPath = BindingPath(currentCatalogPath.steps + path.steps)
    //     val resolvedVar = when (path.steps.size) {
    //         0 -> null
    //         1 -> getGlobalType(currentCatalog, path, absoluteCatalogPath)
    //         2 -> getGlobalType(currentCatalog, path, path) ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
    //         else -> {
    //             val inferredCatalog = path.steps[0]
    //             val newPath = BindingPath(path.steps.subList(1, path.steps.size))
    //             getGlobalType(inferredCatalog, path, newPath)
    //                 ?: getGlobalType(currentCatalog, path, path)
    //                 ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
    //         }
    //     }
    //     return resolvedVar
    // }
    //

    //
    // /**
    //  * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
    //  *
    //  * 1. Matched = RelativePath - Not Found
    //  * 2. Not Found = Input CatalogPath - Output CatalogPath
    //  * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
    //  * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
    //  */
    // private fun calculateMatched(
    //     originalPath: BindingPath,
    //     inputCatalogPath: BindingPath,
    //     outputCatalogPath: List<String>,
    // ): Int {
    //     return originalPath.steps.size + outputCatalogPath.size - inputCatalogPath.steps.size
    // }
}