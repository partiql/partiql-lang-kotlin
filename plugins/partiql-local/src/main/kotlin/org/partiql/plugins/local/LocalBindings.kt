package org.partiql.plugins.local

import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.nio.file.Path

@OptIn(PartiQLValueExperimental::class)
class LocalBindings(
    private val root: Path,
    private val format: LocalFormat,
) : ConnectorBindings {

    override fun getValue(handle: ConnectorObjectHandle): PartiQLValue {
        TODO("Not yet implemented")
    }
}
