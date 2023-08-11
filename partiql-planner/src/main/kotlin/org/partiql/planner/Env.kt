package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.Constants
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionSignature

/**
 * Handle for associating a catalog with the metadata; pair of catalog to data.
 */
internal typealias Handle<T> = Pair<String, T>

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

    // TYPES

    // /**
    //  * Get a Type.Ref from a Type.Atomic
    //  */
    // internal fun resolveType(type: Type.Atomic): Type.Ref {
    //     header.types.forEachIndexed { i, t ->
    //         // need .equals() if we want to include more variants
    //         if (t.symbol == type.symbol) {
    //             return Plan.typeRef(t.symbol, i)
    //         }
    //     }
    //     throw IllegalArgumentException("Catalog does not contain type ${type.symbol}")
    // }
    //
    // /**
    //  * Get a Plan [Type.Ref] from a simple PartiQLValueType
    //  */
    // internal fun resolveType(type: PartiQLValueType): Type.Ref {
    //     val symbol = when (type) {
    //         PartiQLValueType.BOOL -> "bool"
    //         PartiQLValueType.INT8 -> "int8"
    //         PartiQLValueType.INT16 -> "int16"
    //         PartiQLValueType.INT32 -> "int32"
    //         PartiQLValueType.INT64 -> "int64"
    //         PartiQLValueType.INT -> "int"
    //         PartiQLValueType.DECIMAL -> "decimal"
    //         PartiQLValueType.FLOAT32 -> "float32"
    //         PartiQLValueType.FLOAT64 -> "float64"
    //         PartiQLValueType.CHAR -> "char"
    //         PartiQLValueType.STRING -> "string"
    //         PartiQLValueType.SYMBOL -> "symbol"
    //         PartiQLValueType.BINARY -> "binary"
    //         PartiQLValueType.BYTE -> "byte"
    //         PartiQLValueType.BLOB -> "blob"
    //         PartiQLValueType.CLOB -> "clob"
    //         PartiQLValueType.DATE -> "date"
    //         PartiQLValueType.TIME -> "time"
    //         PartiQLValueType.TIMESTAMP -> "timestamp"
    //         PartiQLValueType.INTERVAL -> "interval"
    //         PartiQLValueType.BAG -> "bag"
    //         PartiQLValueType.LIST -> "list"
    //         PartiQLValueType.SEXP -> "sexp"
    //         PartiQLValueType.STRUCT -> "struct"
    //         PartiQLValueType.NULL -> "null"
    //         PartiQLValueType.MISSING -> "missing"
    //         PartiQLValueType.NULLABLE_BOOL -> "bool"
    //         PartiQLValueType.NULLABLE_INT8 -> "int8"
    //         PartiQLValueType.NULLABLE_INT16 -> "int16"
    //         PartiQLValueType.NULLABLE_INT32 -> "int32"
    //         PartiQLValueType.NULLABLE_INT64 -> "int64"
    //         PartiQLValueType.NULLABLE_INT -> "int"
    //         PartiQLValueType.NULLABLE_DECIMAL -> "decimal"
    //         PartiQLValueType.NULLABLE_FLOAT32 -> "float32"
    //         PartiQLValueType.NULLABLE_FLOAT64 -> "float64"
    //         PartiQLValueType.NULLABLE_CHAR -> "char"
    //         PartiQLValueType.NULLABLE_STRING -> "string"
    //         PartiQLValueType.NULLABLE_SYMBOL -> "symbol"
    //         PartiQLValueType.NULLABLE_BINARY -> "binary"
    //         PartiQLValueType.NULLABLE_BYTE -> "byte"
    //         PartiQLValueType.NULLABLE_BLOB -> "blob"
    //         PartiQLValueType.NULLABLE_CLOB -> "clob"
    //         PartiQLValueType.NULLABLE_DATE -> "date"
    //         PartiQLValueType.NULLABLE_TIME -> "time"
    //         PartiQLValueType.NULLABLE_TIMESTAMP -> "timestamp"
    //         PartiQLValueType.NULLABLE_INTERVAL -> "interval"
    //         PartiQLValueType.NULLABLE_BAG -> "bag"
    //         PartiQLValueType.NULLABLE_LIST -> "list"
    //         PartiQLValueType.NULLABLE_SEXP -> "sexp"
    //         PartiQLValueType.NULLABLE_STRUCT -> "struct"
    //     }
    //     val t =  Plan.typeAtomic(symbol)
    //     return resolveType(t)
    // }

    // FUNCTIONS

    /**
     * This will need to be greatly improved upon. We will need to return some kind of pair which has a list of
     * implicit casts to introduce.
     */
    internal fun getFnSignatures(ref: Fn.Ref.Unresolved): List<FunctionSignature> {
        return header.lookup(ref).map { it.second }
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
}
