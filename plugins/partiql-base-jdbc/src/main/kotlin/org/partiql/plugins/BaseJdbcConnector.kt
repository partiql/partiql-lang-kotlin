package org.partiql.plugins

import org.partiql.spi.connector.ConnectorAggProvider
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlConnector
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental

public class BaseJdbcConnector(
    public val client: BaseJdbcClient
) : SqlConnector() {
    override fun getMetadata(session: ConnectorSession): SqlMetadata =
        BaseJdbcMetadata(client, session, InfoSchema.default())

    //Temporary hack: to supply a foo session
    // We need to better abstract the binding with connector
    override fun getBindings(): ConnectorBindings = client.getBinding(
        object: ConnectorSession {
            override fun getQueryId(): String = "q"
            override fun getUserId(): String = "u"
                                 }
    )

    @FnExperimental
    override fun getFunctions(): ConnectorFnProvider =
        super.getFunctions()

    @FnExperimental
    override fun getAggregations(): ConnectorAggProvider = super.getAggregations()
}
