package org.partiql.spi.connector

@RequiresOptIn(
    message = "ConnectorFunction requires explicit opt-in",
    level = RequiresOptIn.Level.ERROR,
)
public annotation class ConnectorFunctionExperimental
