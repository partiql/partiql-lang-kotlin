package org.partiql.plugins.memory

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructElement
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.missingValue

@OptIn(PartiQLValueExperimental::class)
class MemoryBindings(
    private val bindings: Map<String, PartiQLValue>,
) : ConnectorBindings {

    @OptIn(PartiQLValueExperimental::class)
    override fun getValue(handle: ConnectorObjectHandle): PartiQLValue {
        val key = (handle.value as MemoryObject).path.joinToString(".")
        return bindings[key] ?: missingValue()
    }

    internal fun iterator(): Iterator<PartiQLValue> = bindings.values.iterator()

    companion object {

        /**
         * Loads each declared global of the catalog from the data element.
         */
        fun load(catalog: MemoryCatalog, data: StructElement): MemoryBindings {
            val bindings = mutableMapOf<String, PartiQLValue>()
            for (key in catalog.keys()) {
                var ion: IonElement = data
                val steps = key.split(".")
                steps.forEach { s ->
                    if (ion is StructElement) {
                        ion = (ion as StructElement).getOptional(s) ?: error("No value for binding $key")
                    } else {
                        error("No value for binding $key")
                    }
                }
                bindings[key] = PartiQLValueIonReaderBuilder.standard().build(ion).read()
            }
            return MemoryBindings(bindings)
        }
    }
}
