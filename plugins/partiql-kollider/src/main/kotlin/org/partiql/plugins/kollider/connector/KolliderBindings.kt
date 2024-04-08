package org.partiql.plugins.kollider.connector

import org.partiql.plugins.kollider.index.KIndex
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class KolliderBindings(private val index: KIndex) : ConnectorBindings {

    @OptIn(PartiQLValueExperimental::class)
    override fun getValue(path: ConnectorPath): PartiQLValue {
        TODO("Not yet implemented")
    }

}