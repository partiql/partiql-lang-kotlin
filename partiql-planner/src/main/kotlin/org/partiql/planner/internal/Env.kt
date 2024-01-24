package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.catalogSymbolRef
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpGlobal
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.typer.FnResolver
import org.partiql.planner.internal.typer.TypeEnv
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * Handle for associating a catalog name with catalog related metadata objects.
 */
internal typealias Handle<T> = Pair<String, T>

/**
 * Metadata for a resolved global variable
 *
 * @property type       Resolved StaticType
 * @property ordinal    The relevant catalog's index offset in the [Env.catalogs] list
 * @property depth      The depth/level of the path match.
 * @property position   The relevant value's index offset in the [Catalog.values] list
 */
internal class ResolvedVar(
    val type: StaticType,
    val ordinal: Int,
    val depth: Int,
    val position: Int,
)

/**
 * Variable resolution strategies — https://partiql.org/assets/PartiQL-Specification.pdf#page=35
 *
 * | Value      | Strategy              | Scoping Rules |
 * |------------+-----------------------+---------------|
 * | LOCAL      | local-first lookup    | Rules 1, 2    |
 * | GLOBAL     | global-first lookup   | Rule 3        |
 */
internal enum class ResolutionStrategy {
    LOCAL,
    GLOBAL,
}

/**
 * PartiQL Planner Global Environment of Catalogs backed by given plugins.
 *
 * @property session        Session details
 */
internal class Env(
    private val session: PartiQLPlanner.Session,
) {

    /**
     * Collect the list of all referenced globals during planning.
     */
    public val catalogs = mutableListOf<Catalog>()

    /**
     * Catalog Metadata for this query session.
     */
    private val connectors = session.catalogs

    /**
     * Encapsulate all function resolving logic within [FnResolver].
     *
     * TODO we should be using a search_path for resolving functions. This is not possible at the moment, so we flatten
     *      all builtin functions to live at the top-level. At the moment, we could technically use this to have
     *      single-level `catalog`.`function`() syntax but that is out-of-scope for this commit.
     */
    private val fnResolver = FnResolver(connectors.values.mapNotNull { it.functions })

    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = session.queryId
        override fun getUserId(): String = session.userId
    }

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
    private fun getObjectHandle(catalog: BindingName, path: BindingPath): Handle<ConnectorObjectHandle>? {
        val metadata = getMetadata(catalog) ?: return null
        return metadata.second.getObjectHandle(connectorSession, path)?.let {
            metadata.first to it
        }
    }

    /**
     * Fetch a global variable's StaticType given its handle.
     *
     * @param handle
     * @return
     */
    private fun getObjectDescriptor(handle: Handle<ConnectorObjectHandle>): StaticType {
        val metadata = getMetadata(BindingName(handle.first, BindingCase.SENSITIVE))?.second
            ?: error("Unable to fetch connector metadata based on handle $handle")
        return metadata.getObjectType(connectorSession, handle.second) ?: error("Unable to produce Static Type")
    }

    /**
     * Fetch [ConnectorMetadata] given a catalog name.
     *
     * @param catalogName
     * @return
     */
    private fun getMetadata(catalogName: BindingName): Handle<ConnectorMetadata>? {
        val catalogKey = connectors.keys.firstOrNull { catalogName.isEquivalentTo(it) } ?: return null
        val metadata = connectors[catalogKey] ?: return null
        return catalogKey to metadata
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

    /**
     * @return a [Pair] where [Pair.first] is the catalog index and [Pair.second] is the value index within that catalog
     */
    private fun getOrAddCatalogValue(
        catalogName: String,
        valuePath: List<String>,
        valueType: StaticType,
    ): Pair<Int, Int> {
        val catalogIndex = getOrAddCatalog(catalogName)
        val symbols = catalogs[catalogIndex].symbols
        return symbols.indexOfFirst { value ->
            value.path == valuePath
        }.let { index ->
            when (index) {
                -1 -> {
                    catalogs[catalogIndex] = catalogs[catalogIndex].copy(
                        symbols = symbols + listOf(Catalog.Symbol(valuePath, valueType))
                    )
                    catalogIndex to catalogs[catalogIndex].symbols.lastIndex
                }
                else -> {
                    catalogIndex to index
                }
            }
        }
    }

    private fun getOrAddCatalog(catalogName: String): Int {
        return catalogs.indexOfFirst { catalog ->
            catalog.name == catalogName
        }.let {
            when (it) {
                -1 -> {
                    catalogs.add(Catalog(catalogName, emptyList()))
                    catalogs.lastIndex
                }
                else -> it
            }
        }
    }

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
