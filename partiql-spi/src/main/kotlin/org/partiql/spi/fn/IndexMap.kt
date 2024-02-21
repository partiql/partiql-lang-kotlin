package org.partiql.spi.fn

import org.partiql.spi.connector.ConnectorPath

/**
 * An implementation of [Index] which uses the normalized paths as map keys.
 *
 * @property map
 */
internal class IndexMap<T>(private val map: Map<String, Map<String, T>>) : Index<T> {

    override fun get(path: List<String>): List<T> {
        val key = path.joinToString(".")
        val variants = map[key] ?: emptyMap()
        return variants.values.toList()
    }

    override fun get(path: ConnectorPath, specific: String): T? {
        val key = path.steps.joinToString(".")
        val variants = map[key] ?: emptyMap()
        return variants[specific]
    }
}
