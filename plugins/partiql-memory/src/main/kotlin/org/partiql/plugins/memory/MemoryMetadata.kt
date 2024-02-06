package org.partiql.plugins.memory

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental

internal class MemoryMetadata(
    private val catalog: MemoryCatalog,
    session: ConnectorSession,
    info: InfoSchema,
) : SqlMetadata(session, info) {

    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        return super.getObject(path) ?: catalog.find(path)
    }

    @FnExperimental
    override fun getFunction(path: BindingPath): ConnectorHandle.Fn? {
        return super.getFunction(path)
    }
}
