package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
class MemoryBindingsTest {

    @Test
    fun load() {
        val data = loadSingleElement(
            """
            {
              "x": 1,
              "y": "hello",
              "z": {
                "a": true,
                "b": 1.0
              }
            }
            """.trimIndent()
        )
        val catalog = MemoryCatalog(
            mapOf(
                "x" to StaticType.INT,
                "y" to StaticType.STRING,
                "z.a" to StaticType.BOOL,
                "z.b" to StaticType.FLOAT,
            )
        )
        val bindings = MemoryBindings.load(catalog, data as StructElement)
        for (v in bindings.iterator()) {
            println(v)
        }
    }
}
