package org.partiql.plugins.memory

import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.StaticType

class MemoryObject(
    val path: List<String>,
    val type: StaticType
) : ConnectorObject
