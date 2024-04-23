package org.partiql.plugins.fs.connector

import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.StaticType

/**
 * Associate a resolved path with a [StaticType]
 *
 * @property type
 */
internal class FsObject(private val type: StaticType) : ConnectorObject {

    override fun getType(): StaticType = type
}
