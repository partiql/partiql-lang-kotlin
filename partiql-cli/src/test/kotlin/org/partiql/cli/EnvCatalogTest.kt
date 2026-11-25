package org.partiql.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.spi.value.InvalidOperationException
import kotlin.test.assertNotNull

class EnvCatalogTest {

    @Test
    fun structLoadsEachFieldAsATable() {
        val env = Datum.struct(listOf(Field.of("orders", Datum.bag(listOf(Datum.integer(1))))))
        val catalog = MainCommand().envCatalog(env)
        assertNotNull(catalog.getTable(session(), Name.of("orders")))
    }

    @Test
    @Suppress("DEPRECATION")
    fun jsonObjectIsRejected() {
        // A JSON object answers getFields(), like a struct, so only the type check tells them apart.
        val env = Datum.jsonObject(listOf(Field.of("orders", Datum.jsonArray(listOf(Datum.jsonScalar(Datum.integer(1)))))))
        assertThrows<InvalidOperationException> { MainCommand().envCatalog(env) }
    }

    @Test
    fun otherTypesAreRejected() {
        listOf(Datum.integer(1), Datum.bag(emptyList()), Datum.nullValue(PType.struct()), Datum.missing()).forEach {
            assertThrows<InvalidOperationException>(it.toString()) { MainCommand().envCatalog(it) }
        }
    }

    private fun session() = Session.builder().catalog("default").build()
}
