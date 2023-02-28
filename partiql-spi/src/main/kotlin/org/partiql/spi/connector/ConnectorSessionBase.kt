package org.partiql.spi.connector

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
