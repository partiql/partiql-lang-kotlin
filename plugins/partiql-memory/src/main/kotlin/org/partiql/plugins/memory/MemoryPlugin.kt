package org.partiql.plugins.memory

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

class MemoryPlugin(private val catalogs: Map<String, MemoryConnector>) : Plugin {

    override fun getConnectorFactories(): List<Connector.Factory> = listOf(MemoryConnector.Factory(catalogs))

    @PartiQLFunctionExperimental
    override fun getFunctions(): List<PartiQLFunction> = emptyList()
}
