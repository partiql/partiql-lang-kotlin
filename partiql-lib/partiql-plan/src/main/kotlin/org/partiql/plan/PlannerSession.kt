package org.partiql.plan

import org.partiql.spi.connector.ConnectorSession
import java.time.Instant

public class PlannerSession(
    public val queryId: String,
    public val userId: String,
    public val catalog: String?,
    public val schema: String?,
    public val catalogMap: Map<String, String>,
    public val instant: Instant
) {
    public fun toConnectorSession(): ConnectorSession = object : ConnectorSession {
        override fun getQueryId(): String = queryId
        override fun getUserId(): String = userId
    }
}
