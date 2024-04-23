package org.partiql.spi.connector.sql

import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.IdentifierMapping

public open class SqlIdentifierMapping : IdentifierMapping {
    override fun fromBindingPath(bindingPath: BindingPath): ConnectorPath =
        ConnectorPath(bindingPath.normalized)

    override fun fromConnectorPath(connectorPath: ConnectorPath): BindingPath =
        connectorPath.steps.map {
            BindingName(it, BindingCase.SENSITIVE)
        }.let { BindingPath(it) }
}
