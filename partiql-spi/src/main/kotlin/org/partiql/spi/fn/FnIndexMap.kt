package org.partiql.spi.fn

import org.partiql.spi.connector.ConnectorPath

/**
 * An implementation of [FnIndex] which uses the normalized paths as map keys.
 *
 * @property map
 */
@OptIn(FnExperimental::class)
internal class FnIndexMap(private val map: Map<String, Map<String, Fn>>) : FnIndex {

    override fun get(path: List<String>): List<Fn> {
        val key = path.joinToString(".")
        val variants = map[key] ?: emptyMap()
        return variants.values.toList()
    }

    override fun get(path: ConnectorPath, specific: String): Fn? {
        val key = path.steps.joinToString(".")
        val variants = map[key] ?: emptyMap()
        return variants[specific]
    }
}
