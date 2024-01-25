package org.partiql.planner.internal

import org.partiql.spi.connector.ConnectorHandle

/**
 * A simple catalog to metadata pair.
 *
 * @param T
 * @property catalog    The resolved entity's catalog name.
 * @property handle     The resolved entity's catalog path and type information.
 */
internal data class PathItem<T>(
    @JvmField val catalog: String,
    @JvmField val handle: ConnectorHandle<T>,
)
