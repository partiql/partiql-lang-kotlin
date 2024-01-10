package org.partiql.spi.connector.base

import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.spi.connector.ConnectorFunctions

/**
 * The [BaseConnector] provides an abstract base for
 */
public abstract class BaseConnector : Connector {

    @OptIn(ConnectorFunctionExperimental::class)
    override fun getFunctions(): ConnectorFunctions = BaseFunctions()
}
