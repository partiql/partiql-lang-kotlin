package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.Rel
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.Constants
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionSignature

/**
 * Handle for associating a catalog with the metadata; pair of catalog to data.
 */
internal typealias Handle<T> = Pair<String, T>

internal class ResolvedType(
    val type: StaticType,
    val levelsMatched: Int = 1,
)

/**
 * PartiQL Planner Environment of Catalogs backed by given plugins.
 *
 * @property header         List of namespaced definitions
 * @property plugins        List of plugins for global resolution
 * @property session        Session details
 */
internal class Env(
    private val header: Header,
    private val plugins: List<Plugin>,
    private val session: PartiQLPlanner.Session,
) {

    /**
     * Collect the list of all referenced globals during planning.
     */
    public val globals = mutableListOf<Global>()

    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = session.queryId
        override fun getUserId(): String = session.userId
    }

    /**
     * Map of catalog names to its underlying connector
     */
    private val catalogs: Map<String, Connector>

    // Initialize connectors
    init {
        val catalogs = mutableMapOf<String, Connector>()
        val connectors = plugins.flatMap { it.getConnectorFactories() }
        // map catalogs to connectors
        for ((catalog, config) in session.catalogConfig) {
            // find corresponding connector
            val connectorName = config[Constants.CONFIG_KEY_CONNECTOR_NAME].stringValue
            val connector = connectors.first { it.getName() == connectorName }
            // initialize connector with given config
            catalogs[catalog] = connector.create(catalog, config)
        }
        this.catalogs = catalogs.toMap()
    }

    /**
     * This will need to be greatly improved upon. We will need to return some kind of pair which has a list of
     * implicit casts to introduce.
     */
    internal fun getFnSignatures(ref: Fn.Unresolved): List<FunctionSignature> {
        return header.lookup(ref)
    }

    /**
     * TODO
     */
    internal fun getFnAggHandle(identifier: Identifier): Nothing = TODO()

    /**
     * Fetch global object metadata from the given [BindingPath].
     *
     * @param catalog   Current catalog
     * @param path      Global identifier path
     * @return
     */
    internal fun getObjectHandle(catalog: BindingName, path: BindingPath): Handle<ConnectorObjectHandle>? {
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
    internal fun getObjectDescriptor(handle: Handle<ConnectorObjectHandle>): StaticType {
        val metadata = getMetadata(BindingName(handle.first, BindingCase.SENSITIVE))!!.second
        return metadata.getObjectType(connectorSession, handle.second)!!
    }

    /**
     * Fetch [ConnectorMetadata] given a catalog name.
     *
     * @param catalogName
     * @return
     */
    private fun getMetadata(catalogName: BindingName): Handle<ConnectorMetadata>? {
        val catalogKey = catalogs.keys.firstOrNull { catalogName.isEquivalentTo(it) } ?: return null
        val connector = catalogs[catalogKey] ?: return null
        val metadata = connector.getMetadata(connectorSession)
        return catalogKey to metadata
    }

    /**
     * TODO
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getType(
        catalog: BindingName?,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ): ResolvedType? {
        return catalog?.let { cat ->
            getObjectHandle(cat, catalogPath)?.let { handle ->
                getObjectDescriptor(handle).let {
                    val matched = calculateMatched(originalPath, catalogPath, handle.second.absolutePath)
                    ResolvedType(it, levelsMatched = matched)
                }
            }
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
    internal fun resolveGlobalBind(path: BindingPath): ResolvedType? {
        val currentCatalog = session.currentCatalog?.let { BindingName(it, BindingCase.SENSITIVE) }
        val currentCatalogPath = BindingPath(session.currentDirectory.map { BindingName(it, BindingCase.SENSITIVE) })
        val absoluteCatalogPath = BindingPath(currentCatalogPath.steps + path.steps)
        return when (path.steps.size) {
            0 -> null
            1 -> getType(currentCatalog, path, absoluteCatalogPath)
            2 -> getType(currentCatalog, path, path) ?: getType(currentCatalog, path, absoluteCatalogPath)
            else -> {
                val inferredCatalog = path.steps[0]
                val newPath = BindingPath(path.steps.subList(1, path.steps.size))
                getType(inferredCatalog, path, newPath)
                    ?: getType(currentCatalog, path, path)
                    ?: getType(currentCatalog, path, absoluteCatalogPath)
            }
        }
    }

    /**
     * Logic is as follows:
     * 1. Look through [input] to find the root of the [path]. If found, return. Else, go to step 2.
     * 2. Look through [input] and grab all [StructType]s. Then, grab the fields of each Struct corresponding to the
     *  root of [path].
     *  - If the Struct if ordered, grab the first matching field.
     *  - If unordered and if multiple fields found, merge the output type. If no structs contain a matching field, return null.
     */
    internal fun resolveLocalBind(path: BindingPath, input: List<Rel.Binding>): ResolvedType? {
        if (path.steps.isEmpty()) {
            return null
        }
        val root: StaticType = input.firstOrNull {
            path.steps[0].isEquivalentTo(it.name)
        }?.type ?: run {
            input.map { it.type }.filterIsInstance<StructType>().mapNotNull { struct ->
                inferStructLookup(struct, path.steps[0])
            }.let { potentialTypes ->
                when (potentialTypes.size) {
                    1 -> potentialTypes.first()
                    else -> null
                }
            }
        } ?: return null
        return ResolvedType(root)
    }

    /**
     * Searches for the [key] in the [struct]. If not found, return null
     */
    internal fun inferStructLookup(
        struct: StructType,
        key: BindingName,
    ): StaticType? = when (struct.constraints.contains(TupleConstraint.Ordered)) {
        true -> struct.fields.firstOrNull { entry ->
            key.isEquivalentTo(entry.key)
        }?.value
        false -> struct.fields.mapNotNull { entry ->
            entry.value.takeIf { key.isEquivalentTo(entry.key) }
        }.let { valueTypes ->
            StaticType.unionOf(valueTypes.toSet()).flatten().takeIf { valueTypes.isNotEmpty() }
        }
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
}
