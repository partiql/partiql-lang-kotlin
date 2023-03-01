package org.partiql.plan

import org.partiql.spi.connector.ConnectorSession
import java.time.Instant

public class PlannerSession2(
    public val queryId: String,
    public val userId: String,
    public val connector: String,
    public val instant: Instant
) {
    public fun toConnectorSession(): ConnectorSession = object : ConnectorSession {
        override fun getQueryId(): String = queryId
        override fun getUserId(): String = userId
    }
}
