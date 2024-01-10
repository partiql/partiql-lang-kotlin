package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorFunctions
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.base.BaseConnector
import org.partiql.spi.connector.base.BaseConnectorMetadata
import org.partiql.spi.connector.base.BaseMetadata
import org.partiql.types.StaticType

/**
 * This is a plugin used for testing and is not a versioned API per semver.
 */
public class MemoryConnector(
    private val metadata: ConnectorMetadata,
    private val bindings: ConnectorBindings,
) : BaseConnector() {

    companion object {
        const val CONNECTOR_NAME = "memory"
    }

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = metadata

    override fun getBindings(): ConnectorBindings = bindings

    class Factory(private val catalogs: Map<String, MemoryConnector>) : Connector.Factory {

        override val name: String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement?): Connector {
            return catalogs[catalogName] ?: error("Catalog $catalogName is not registered in the MemoryPlugin")
        }
    }

    /**
     * Connector metadata uses dot-delimited identifiers and StaticType for catalog metadata.
     *
     * @property map
     */
    class Metadata(private val map: Map<String, StaticType>) : BaseMetadata() {

        public val entries: List<Pair<String, StaticType>> = map.entries.map { it.key to it.value }

        companion object {
            @JvmStatic
            fun of(vararg entities: Pair<String, StaticType>) = Metadata(mapOf(*entities))
        }

        override fun getObject(path: BindingPath): ConnectorHandle<ConnectorObject>? {
            val value = lookup(path) ?: return null
            return ConnectorHandle(
                path = ConnectorPath(value.path),
                entity = value,
            )
        }

        operator fun get(key: String): StaticType? = map[key]

        public fun lookup(path: BindingPath): MemoryObject? {
            val kPath = path.normalized
            val k = path.key
            if (this[k] != null) {
                return this[k]?.let { MemoryObject(kPath, it) }
            } else {
                val candidatePath = this.map.keys.map { it.split(".") }
                val kPathIter = kPath.listIterator()
                while (kPathIter.hasNext()) {
                    val currKPath = kPathIter.next()
                    candidatePath.forEach {
                        val match = mutableListOf<String>()
                        val candidateIterator = it.iterator()
                        while (candidateIterator.hasNext()) {
                            if (candidateIterator.next() == currKPath) {
                                match.add(currKPath)
                                val pathIteratorCopy = kPath.listIterator(kPathIter.nextIndex())
                                candidateIterator.forEachRemaining {
                                    val nextPath = pathIteratorCopy.next()
                                    if (it != nextPath) {
                                        match.clear()
                                        return@forEachRemaining
                                    }
                                    match.add(it)
                                }
                            } else {
                                return@forEach
                            }
                        }
                        if (match.isNotEmpty()) {
                            return this[match.joinToString(".")]?.let { it1 ->
                                MemoryObject(
                                    match,
                                    it1
                                )
                            }
                        }
                    }
                }
                return null
            }
        }
    }
}
