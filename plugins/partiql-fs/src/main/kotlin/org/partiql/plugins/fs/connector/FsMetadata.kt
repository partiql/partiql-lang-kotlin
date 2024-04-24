package org.partiql.plugins.fs.connector

import org.partiql.plugins.fs.index.FsIndex
import org.partiql.plugins.fs.index.FsNode
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema

internal class FsMetadata(
    private val index: FsIndex,
    private val info: InfoSchema,
) : SqlMetadata(ignore, info) {

    private companion object {

        // TODO remove session from SqlMetadata.
        @JvmStatic
        val ignore = object : ConnectorSession {
            override fun getQueryId(): String = ""
            override fun getUserId(): String = ""
        }
    }

    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        val (obj, match) = index.search(path) ?: return null
        return ConnectorHandle.Obj(
            path = ConnectorPath(match),
            entity = FsObject(obj.shape)
        )
    }

    override fun ls(path: BindingPath): List<ConnectorHandle<*>> {
        return index.list(path).map {
            when (it) {
                is FsNode.Obj -> ConnectorHandle.Obj(
                    path = ConnectorPath(path.normalized + it.name),
                    entity = FsObject(it.shape)
                )
                is FsNode.Scope -> ConnectorHandle.Scope(
                    path = ConnectorPath(path.normalized + it.name),
                    entity = it.name,
                )
            }
        }
    }
}
