package org.partiql.plugins.memory

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

class MemoryPlugin(private val catalogs: Map<String, MemoryConnector>) : Plugin {

    override val factory: Connector.Factory = MemoryConnector.Factory(catalogs)

    @PartiQLFunctionExperimental
    override val functions: List<PartiQLFunction> = emptyList()
}
