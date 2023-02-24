package org.partiql.lang.session

import org.partiql.spi.connector.ConnectorSession

class ConnectorSessionBase(
    private val queryId: String,
    private val userId: String
) : ConnectorSession {
    override fun getQueryId(): String {
        return queryId
    }

    override fun getUserId(): String {
        return userId
    }
}
