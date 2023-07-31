package org.partiql.cli.utils

import org.partiql.lang.eval.EvaluationSession
import org.partiql.spi.connector.ConnectorSession
import java.util.UUID

class EvaluationSessionConnector(private val evaluationSession: EvaluationSession) : ConnectorSession {
    private val queryId: String = UUID.randomUUID().toString()

    override fun getQueryId(): String = queryId

    override fun getUserId(): String {
        val user = evaluationSession.context[EvaluationSession.Constants.CURRENT_USER_KEY]
        return if (user is String) user else "partiql_unknown_user"
    }
}

fun EvaluationSession.toConnectorSession(): ConnectorSession {
    return EvaluationSessionConnector(this)
}
