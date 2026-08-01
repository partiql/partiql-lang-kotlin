package org.partiql.eval.internal.operator.rex

import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.test.assertEquals

class CastTableTest {
    @Test
    fun printCastTable() {
        println(CastTable)
    }

    @Test
    fun varcharDoesNotPadValues() {
        val actual = CastTable.cast(Datum.string("a"), PType.varchar(20))

        assertEquals("a", actual.string)
    }
}
