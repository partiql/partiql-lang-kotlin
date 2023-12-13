package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

class MemoryPlugin(
    val provider: MemoryCatalog.Provider,
    val data: StructElement? = null,
) : Plugin {

    override val factory: Connector.Factory = MemoryConnector.Factory(provider, data)

    @PartiQLFunctionExperimental
    override val functions: List<PartiQLFunction> = emptyList()
}
