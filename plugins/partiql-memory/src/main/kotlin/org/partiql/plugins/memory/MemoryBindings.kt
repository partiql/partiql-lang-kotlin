package org.partiql.plugins.memory

import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.missingValue

@OptIn(PartiQLValueExperimental::class)
public class MemoryBindings(private val catalog: MemoryCatalog) : ConnectorBindings {

    override fun getValue(path: ConnectorPath): PartiQLValue {
        return catalog.get(path)?.getValue() ?: missingValue()
    }
}
