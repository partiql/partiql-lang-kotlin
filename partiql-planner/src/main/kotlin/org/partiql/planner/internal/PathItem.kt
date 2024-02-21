package org.partiql.planner.internal

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle

/**
 * A simple catalog to metadata pair.
 *
 * @param T
 * @property catalog    The resolved entity's catalog name.
 * @property input      The input binding path (sent to SPI) that resulted in this item match.
 * @property handle     The resolved entity's catalog path and type information.
 */
internal data class PathItem<T>(
    @JvmField val catalog: String,
    @JvmField val input: BindingPath,
    @JvmField val handle: ConnectorHandle<T>,
)
