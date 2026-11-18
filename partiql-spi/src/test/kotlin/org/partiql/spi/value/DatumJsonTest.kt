package org.partiql.spi.value

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.types.PType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class DatumJsonTest {

    private val comparator = Datum.comparator()

    private fun scalar(v: Datum) = Datum.jsonScalar(v)
    private fun obj(vararg members: Pair<String, Datum>) = Datum.jsonObject(members.map { Field.of(it.first, it.second) })
    private fun arr(vararg elements: Datum) = Datum.jsonArray(elements.toList())

    @Test
    fun typeHasCodeNameAndRoundTrips() {
        assertEquals(PType.JSON, PType.json().code())
        assertEquals("JSON", PType.json().name())
        assertEquals(PType.json(), PType.of(PType.JSON))
        assertTrue(PType.codes().contains(PType.JSON))
    }

    @Test
    fun sqlJsonNullIsAValueNotSqlNull() {
        val jsonNull = Datum.jsonNull()
        assertEquals(PType.JSON, jsonNull.type.code())
        assertEquals(JsonKind.NULL, jsonNull.jsonKind)
        assertFalse(jsonNull.isNull)
        assertFalse(jsonNull.isMissing)
    }

    @Test
    fun sqlNullOfTypeJson() {
        val sqlNull = Datum.nullValue(PType.json())
        assertEquals(PType.JSON, sqlNull.type.code())
        assertTrue(sqlNull.isNull)
    }

    @Test
    fun scalarKeepsItsSqlValue() {
        val s = scalar(Datum.date(java.time.LocalDate.of(2024, 1, 1)))
        assertEquals(JsonKind.SCALAR, s.jsonKind)
        assertEquals(PType.DATE, s.jsonScalar.type.code())
        assertEquals(7, scalar(Datum.integer(7)).jsonScalar.int)
    }

    @Test
    fun scalarRejectsNullMissingAndOtherTypes() {
        assertThrows<IllegalArgumentException> { scalar(Datum.nullValue(PType.integer())) }
        assertThrows<IllegalArgumentException> { scalar(Datum.missing()) }
        assertThrows<IllegalArgumentException> { scalar(Datum.struct()) }
        assertThrows<IllegalArgumentException> { scalar(Datum.jsonNull()) }
    }

    @Test
    fun scalarAcceptsOnlyTheTypesJsonNeedsForNow() {
        listOf(
            Datum.string("x"), Datum.integer(1), Datum.bigint(1L), Datum.decimal(java.math.BigDecimal("10.5"), 3, 1),
            Datum.doublePrecision(1e3), Datum.bool(true), Datum.date(java.time.LocalDate.of(2024, 1, 1)),
        ).forEach { assertEquals(JsonKind.SCALAR, scalar(it).jsonKind) }
        assertThrows<IllegalArgumentException> { scalar(Datum.smallint(1)) }
        assertThrows<IllegalArgumentException> { scalar(Datum.real(1f)) }
        assertThrows<IllegalArgumentException> { scalar(Datum.varchar("x", 5)) }
    }

    @Test
    fun objectKeepsOrderAndRepeatedKeys() {
        val o = obj("a" to scalar(Datum.integer(1)), "b" to Datum.jsonNull(), "a" to scalar(Datum.integer(2)))
        assertEquals(JsonKind.OBJECT, o.jsonKind)
        val members = o.fields.asSequence().toList()
        assertEquals(listOf("a", "b", "a"), members.map { it.name })
        assertEquals(1, members[0].value.jsonScalar.int)
        assertEquals(2, members[2].value.jsonScalar.int)
    }

    @Test
    fun arrayKeepsOrder() {
        val a = arr(scalar(Datum.integer(1)), Datum.jsonNull(), obj())
        assertEquals(JsonKind.ARRAY, a.jsonKind)
        assertEquals(listOf(JsonKind.SCALAR, JsonKind.NULL, JsonKind.OBJECT), a.iterator().asSequence().map { it.jsonKind }.toList())
    }

    @Test
    fun partsMustBeJsonItems() {
        assertThrows<IllegalArgumentException> { arr(Datum.integer(1)) }
        assertThrows<IllegalArgumentException> { arr(Datum.nullValue(PType.json())) }
        assertThrows<IllegalArgumentException> { arr(Datum.missing()) }
        assertThrows<IllegalArgumentException> { obj("a" to Datum.string("x")) }
        assertThrows<IllegalArgumentException> { obj("a" to Datum.nullValue(PType.json())) }
    }

    @Test
    fun accessorsOfAnotherKindThrow() {
        assertThrows<InvalidOperationException> { Datum.jsonNull().jsonScalar }
        assertThrows<InvalidOperationException> { scalar(Datum.integer(1)).fields }
        assertThrows<InvalidOperationException> { obj().iterator() }
        assertThrows<InvalidOperationException> { Datum.integer(1).jsonKind }
    }

    @Test
    fun comparingJsonIsNotSupportedYet() {
        assertThrows<UnsupportedOperationException> { comparator.compare(scalar(Datum.integer(1)), scalar(Datum.integer(1))) }
        assertThrows<UnsupportedOperationException> { comparator.compare(Datum.jsonNull(), obj()) }
        assertThrows<UnsupportedOperationException> { comparator.compare(Datum.integer(1), Datum.jsonNull()) }
        assertThrows<UnsupportedOperationException> { comparator.compare(arr(), Datum.struct()) }
    }

    @Test
    fun sqlNullOfTypeJsonStillComparesAsNull() {
        assertEquals(0, comparator.compare(Datum.nullValue(PType.json()), Datum.nullValue()))
        assertTrue(comparator.compare(Datum.nullValue(PType.json()), Datum.integer(1)) != 0)
    }
}
