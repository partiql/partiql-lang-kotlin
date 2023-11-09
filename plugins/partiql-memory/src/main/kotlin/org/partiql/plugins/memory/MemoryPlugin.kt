package org.partiql.plugins.memory

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

class MemoryPlugin(val provider: MemoryCatalog.Provider) : Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> = listOf(MemoryConnector.Factory(provider))

    @PartiQLFunctionExperimental
    override fun getFunctions(): List<PartiQLFunction> = emptyList()
}
