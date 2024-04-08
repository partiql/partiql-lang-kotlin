package org.partiql.plugins.kollider.connector

import org.partiql.plugins.kollider.index.KIndex
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema

internal class KolliderMetadata(
    private val index: KIndex,
    private val info: InfoSchema,
) : SqlMetadata(info) {

    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        val (obj, match) = index.lookup(path) ?: return null
        return ConnectorHandle.Obj(
            path = ConnectorPath(match),
            entity = KolliderObject(obj.shape)
        )
    }
}
