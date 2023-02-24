package org.partiql.lang.infer

import org.partiql.spi.connector.ConnectorSession

public class Session(
    public val queryId: String,
    public val catalog: String?,
    public val schema: String?
) {
    public fun toConnectorSession(): ConnectorSession = object : ConnectorSession {
        override fun getQueryId(): String = queryId

        // TODO
        override fun getUserId(): String = "UNKNOWN"
    }
}
