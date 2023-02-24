package org.partiql.plan

import org.partiql.spi.connector.ConnectorTableHandle

class TableHandle(
    val connectorHandle: ConnectorTableHandle,
    val catalogName: String
)
