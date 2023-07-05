package org.partiql.plugins.bananna

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction

class BanannaPlugin : Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> {
        return emptyList()
    }

    override fun getFunctions(): List<PartiQLFunction> = listOf(
        TrimLeading
    )
}
