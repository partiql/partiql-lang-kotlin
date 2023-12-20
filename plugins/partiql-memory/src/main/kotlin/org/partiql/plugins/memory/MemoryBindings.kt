package org.partiql.plugins.memory

import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.missingValue

@OptIn(PartiQLValueExperimental::class)
class MemoryBindings(
    private val bindings: Map<String, PartiQLValue>,
) : ConnectorBindings {

    @OptIn(PartiQLValueExperimental::class)
    override fun getValue(path: ConnectorObjectPath): PartiQLValue {
        val key = path.steps.joinToString(".")
        return bindings[key] ?: missingValue()
    }

    companion object {

        /**
         * No bindings.
         */
        val empty = MemoryBindings(emptyMap())
    }
}
