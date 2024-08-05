package org.partiql.plugins.local

import org.partiql.planner.catalog.Name
import org.partiql.spi.connector.ConnectorBinding
import org.partiql.spi.connector.ConnectorBindings

internal object LocalBindings : ConnectorBindings {

    override fun getBinding(name: Name): ConnectorBinding? = null
}
