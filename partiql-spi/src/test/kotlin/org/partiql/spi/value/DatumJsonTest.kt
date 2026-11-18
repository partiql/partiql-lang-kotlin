package org.partiql.spi.value

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class DatumJsonTest {

    private val comparator = Datum.comparator()

    private fun scalar(v: Datum) = Datum.jsonScalar(v)
    private fun obj(vararg members: Pair<String, Datum>) = Datum.jsonObject(members.map { Field.of(it.first, it.second) })
    private fun arr(vararg elements: Datum) = Datum.jsonArray(elements.toList())

    companion object {

        /**
         * The types an SQL/JSON scalar accepts. Every other runtime type is rejected.
         */
        private val SCALAR_TYPES = setOf(
            PType.STRING, PType.INTEGER, PType.BIGINT, PType.DECIMAL, PType.DOUBLE, PType.BOOL,
            PType.DATE, PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ,
        )

        private val TIME = LocalTime.of(2, 30, 59)
        private val OFFSET = ZoneOffset.ofHours(-8)

        /**
         * One non-null value of each type that can occur at runtime. DYNAMIC and UNKNOWN have no such values.
         */
        private fun sample(code: Int): Datum? = when (code) {
            PType.DYNAMIC, PType.UNKNOWN -> null
            PType.BOOL -> Datum.bool(true)
            PType.TINYINT -> Datum.tinyint(1)
            PType.SMALLINT -> Datum.smallint(1)
            PType.INTEGER -> Datum.integer(1)
            PType.BIGINT -> Datum.bigint(1L)
            PType.NUMERIC -> Datum.numeric(BigDecimal.ONE)
            PType.DECIMAL -> Datum.decimal(BigDecimal("10.5"), 3, 1)
            PType.REAL -> Datum.real(1f)
            PType.DOUBLE -> Datum.doublePrecision(1e3)
            PType.CHAR -> Datum.character("x", 1)
            PType.VARCHAR -> Datum.varchar("x", 5)
            PType.STRING -> Datum.string("x")
            PType.BLOB -> Datum.blob(byteArrayOf(1))
            PType.CLOB -> Datum.clob(byteArrayOf(1))
            PType.DATE -> Datum.date(LocalDate.of(2024, 1, 1))
            PType.TIME -> Datum.time(TIME, 0)
            PType.TIMEZ -> Datum.timez(OffsetTime.of(TIME, OFFSET), 0)
            PType.TIMESTAMP -> Datum.timestamp(LocalDateTime.of(LocalDate.of(2024, 1, 1), TIME), 0)
            PType.TIMESTAMPZ -> Datum.timestampz(OffsetDateTime.of(LocalDate.of(2024, 1, 1), TIME, OFFSET), 0)
            PType.ARRAY -> Datum.array(listOf(Datum.integer(1)))
            PType.BAG -> Datum.bag(listOf(Datum.integer(1)))
            PType.ROW -> Datum.row(Field.of("a", Datum.integer(1)))
            PType.STRUCT -> Datum.struct(Field.of("a", Datum.integer(1)))
            PType.VARIANT -> Datum.ion("1")
            PType.INTERVAL_YM -> Datum.intervalYear(1, 2)
            PType.INTERVAL_DT -> Datum.intervalDay(1, 2)
            PType.MAP -> Datum.map(PType.string(), PType.integer(), listOf(Entry.of(Datum.string("a"), Datum.integer(1))))
            PType.JSON -> Datum.jsonNull()
            else -> error("No sample for type code $code; add one so the scalar boundary stays pinned")
        }

        @JvmStatic
        fun typeCodes() = PType.codes().toList()
    }

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
        // As for any type, an accessor on SQL NULL throws NullPointerException, not InvalidOperationException.
        assertThrows<NullPointerException> { sqlNull.jsonKind }
        assertThrows<NullPointerException> { sqlNull.jsonScalar }
        assertThrows<NullPointerException> { sqlNull.iterator() }
        assertThrows<NullPointerException> { sqlNull.fields }
    }

    @ParameterizedTest
    @MethodSource("typeCodes")
    fun scalarAcceptsOnlyItsTypes(code: Int) {
        val value = sample(code) ?: return
        if (code in SCALAR_TYPES) {
            val s = scalar(value)
            assertEquals(JsonKind.SCALAR, s.jsonKind)
            assertEquals(code, s.jsonScalar.type.code())
        } else {
            assertThrows<IllegalArgumentException> { scalar(value) }
        }
    }

    @Test
    fun scalarRejectsNullAndMissing() {
        assertThrows<IllegalArgumentException> { scalar(Datum.nullValue(PType.integer())) }
        assertThrows<IllegalArgumentException> { scalar(Datum.missing()) }
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
    fun objectReadsEachMemberOnce() {
        var reads = 0
        val shifty = object : Field {
            override fun getName() = "a"
            override fun getValue(): Datum = if (reads++ == 0) Datum.jsonNull() else Datum.integer(1)
        }
        val o = Datum.jsonObject(listOf(shifty))
        assertEquals(JsonKind.NULL, o.fields.next().value.jsonKind)
        assertEquals(JsonKind.NULL, o.fields.next().value.jsonKind)
    }

    @Test
    fun arrayKeepsOrder() {
        val a = arr(scalar(Datum.integer(1)), Datum.jsonNull(), obj())
        assertEquals(JsonKind.ARRAY, a.jsonKind)
        assertEquals(listOf(JsonKind.SCALAR, JsonKind.NULL, JsonKind.OBJECT), a.iterator().asSequence().map { it.jsonKind }.toList())
    }

    @Test
    fun partsMustBeItemsBuiltByTheFactories() {
        val forged = object : Datum {
            override fun getType(): PType = PType.json()
        }
        listOf(
            Datum.integer(1), Datum.nullValue(PType.json()), Datum.missing(), Datum.missing(PType.json()), forged,
        ).forEach { part ->
            assertThrows<IllegalArgumentException>("element $part") { arr(part) }
            assertThrows<IllegalArgumentException>("member value $part") { obj("a" to part) }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun nullMembersAndElementsAreRejected() {
        assertThrows<IllegalArgumentException> { Datum.jsonObject(listOf(null) as List<Field>) }
        assertThrows<IllegalArgumentException> { Datum.jsonArray(listOf(null) as List<Datum>) }
    }

    @Test
    fun accessorsOfTheWrongKindThrow() {
        val values = mapOf(
            JsonKind.NULL to Datum.jsonNull(),
            JsonKind.SCALAR to scalar(Datum.integer(1)),
            JsonKind.ARRAY to arr(),
            JsonKind.OBJECT to obj(),
        )
        values.forEach { (kind, v) ->
            if (kind != JsonKind.SCALAR) assertThrows<InvalidOperationException>("getJsonScalar on $kind") { v.jsonScalar }
            if (kind != JsonKind.ARRAY) assertThrows<InvalidOperationException>("iterator on $kind") { v.iterator() }
            if (kind != JsonKind.OBJECT) assertThrows<InvalidOperationException>("getFields on $kind") { v.fields }
        }
        assertThrows<InvalidOperationException> { Datum.integer(1).jsonKind }
        assertThrows<InvalidOperationException> { Datum.integer(1).jsonScalar }
    }

    @Test
    fun comparingJsonIsNotSupportedYet() {
        listOf(
            scalar(Datum.integer(1)) to scalar(Datum.integer(1)),
            Datum.jsonNull() to obj(),
            Datum.integer(1) to Datum.jsonNull(),
            arr() to Datum.struct(),
        ).forEach { (l, r) ->
            val e = assertThrows<PRuntimeException> { comparator.compare(l, r) }
            assertEquals(PError.FEATURE_NOT_SUPPORTED, e.error.code())
        }
    }

    @Test
    fun sqlNullOfTypeJsonStillComparesAsNull() {
        assertEquals(0, comparator.compare(Datum.nullValue(PType.json()), Datum.nullValue()))
        // Datum.comparator() puts nulls first.
        assertTrue(comparator.compare(Datum.nullValue(PType.json()), Datum.integer(1)) < 0)
    }
}
