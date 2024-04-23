package org.partiql.spi.connector

import org.partiql.spi.BindingPath

/**
 *
 * Logics that encapsulate translation between [BindingPath] and [ConnectorPath].
 *
 */
public interface IdentifierMapping {

    public fun fromBindingPath(bindingPath: BindingPath): ConnectorPath

    public fun fromConnectorPath(connectorPath: ConnectorPath): BindingPath
}
