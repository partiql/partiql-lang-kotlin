package org.partiql.plugins.memory

import org.partiql.spi.connector.ConnectorObjectType
import org.partiql.types.StaticType

class MemoryObject(
    val path: List<String>,
    val type: StaticType
) : ConnectorObjectType
