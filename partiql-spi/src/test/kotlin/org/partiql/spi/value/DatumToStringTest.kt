package org.partiql.spi.value

import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Test cases for toString methods of all Datum implementations.
 * Verifies that each Datum class produces expected string representations.
 */
class DatumToStringTest {

    @Test
    fun testDatumBooleanToString() {
        val datumTrue = Datum.bool(true)
        val datumFalse = Datum.bool(false)

        assertEquals("DatumBoolean{_type=BOOL, _value=true}", datumTrue.toString())
        assertEquals("DatumBoolean{_type=BOOL, _value=false}", datumFalse.toString())
    }

    @Test
    fun testDatumByteToString() {
        val datum = Datum.tinyint(42.toByte())

        assertEquals("DatumByte{_type=TINYINT, _value=42}", datum.toString())
    }

    @Test
    fun testDatumShortToString() {
        val datum = Datum.smallint(1234.toShort())

        assertEquals("DatumShort{_type=SMALLINT, _value=1234}", datum.toString())
    }

    @Test
    fun testDatumIntToString() {
        val datum = Datum.integer(42)

        assertEquals("DatumInt{_type=INTEGER, _value=42}", datum.toString())
    }

    @Test
    fun testDatumLongToString() {
        val datum = Datum.bigint(123456789L)

        assertEquals("DatumLong{_type=BIGINT, _value=123456789}", datum.toString())
    }

    @Test
    fun testDatumFloatToString() {
        val datum = Datum.real(3.14f)

        assertEquals("DatumFloat{_type=REAL, _value=3.14}", datum.toString())
    }

    @Test
    fun testDatumDoubleToString() {
        val datum = Datum.doublePrecision(2.718)

        assertEquals("DatumDouble{_type=DOUBLE, _value=2.718}", datum.toString())
    }

    @Test
    fun testDatumDecimalToString() {
        val datum = Datum.decimal(BigDecimal("123.45"), 5, 2)

        assertEquals("DatumDecimal{_type=DECIMAL(5, 2), _value=123.45}", datum.toString())
    }

    @Test
    fun testDatumStringToString() {
        val datum = Datum.string("hello world")

        assertEquals("DatumString{_type=STRING, _value=hello world}", datum.toString())
    }

    @Test
    fun testDatumCharsToString() {
        val datum = DatumChars("test", 10)

        assertEquals("DatumChars{_type=CHAR(10), _value=test}", datum.toString())
    }

    @Test
    fun testDatumBytesToString() {
        val datum = Datum.blob(byteArrayOf(10, 50, 100, 127))

        assertEquals("DatumBytes{_type=BLOB(${Integer.MAX_VALUE}), _value=0A32647F}", datum.toString())
    }

    @Test
    fun testDatumDateToString() {
        val datum = Datum.date(LocalDate.of(2023, 12, 25))

        assertEquals("DatumDate{_type=DATE, _value=2023-12-25}", datum.toString())
    }

    @Test
    fun testDatumTimeToString() {
        val datum = Datum.time(LocalTime.of(14, 30, 45), 0)

        assertEquals("DatumTime{_type=TIME(0), _value=14:30:45}", datum.toString())
    }

    @Test
    fun testDatumTimezToString() {
        val datum = Datum.timez(OffsetTime.of(14, 30, 45, 0, ZoneOffset.UTC), 0)

        assertEquals("DatumTimez{_type=TIMEZ(0), _value=14:30:45Z}", datum.toString())
    }

    @Test
    fun testDatumTimestampToString() {
        val datum = Datum.timestamp(LocalDateTime.of(2023, 12, 25, 14, 30, 45), 0)

        assertEquals("DatumTimestamp{_type=TIMESTAMP(0), _value=2023-12-25T14:30:45}", datum.toString())
    }

    @Test
    fun testDatumTimestampzToString() {
        val datum = Datum.timestampz(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.UTC), 0)

        assertEquals("DatumTimestampz{_type=TIMESTAMPZ(0), _value=2023-12-25T14:30:45Z}", datum.toString())
    }

    @Test
    fun testDatumIntervalYearMonthToString() {
        val datum = Datum.intervalYearMonth(2, 6, 2)

        assertEquals("DatumIntervalYearMonth{_type=YEAR (2) TO MONTH, _value=INTERVAL '2-6'}", datum.toString())
    }

    @Test
    fun testDatumIntervalDayTimeToString() {
        val datum = Datum.intervalDaySecond(1, 2, 30, 45, 500000000, 2, 6)

        assertEquals("DatumIntervalDayTime{_type=DAY (2) TO SECOND (6), _value=INTERVAL '1 2:30:45.500000000'}", datum.toString())
    }

    @Test
    fun testDatumCollectionToString() {
        val datumWithSingleType = Datum.array(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)))
        val datumWithMixedType = Datum.array(listOf(Datum.integer(1), Datum.string("hello world")))
        val datumWithNestedType = Datum.array(listOf(Datum.integer(1), datumWithSingleType, datumWithMixedType))

        assertEquals("DatumCollection{_type=ARRAY(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumInt{_type=INTEGER, _value=2}, DatumInt{_type=INTEGER, _value=3}]}", datumWithSingleType.toString())
        assertEquals("DatumCollection{_type=ARRAY(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}", datumWithMixedType.toString())
        assertEquals(
            "DatumCollection{_type=ARRAY(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, " +
                "DatumCollection{_type=ARRAY(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumInt{_type=INTEGER, _value=2}, DatumInt{_type=INTEGER, _value=3}]}, " +
                "DatumCollection{_type=ARRAY(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}]}",
            datumWithNestedType.toString()
        )
    }

    @Test
    fun testDatumBagToString() {
        val datumWithSingleType = Datum.bag(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)))
        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))
        val datumWithNestedType = Datum.bagVararg(Datum.integer(1), datumWithSingleType, datumWithMixedType)

        assertEquals("DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumInt{_type=INTEGER, _value=2}, DatumInt{_type=INTEGER, _value=3}]}", datumWithSingleType.toString())
        assertEquals("DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}", datumWithMixedType.toString())
        assertEquals(
            "DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, " +
                "DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumInt{_type=INTEGER, _value=2}, DatumInt{_type=INTEGER, _value=3}]}, " +
                "DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}]}",
            datumWithNestedType.toString()
        )
    }

    @Test
    fun testDatumStructToString() {
        val fields = listOf(
            Field.of("name", Datum.string("John")),
            Field.of("age", Datum.integer(30))
        )
        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))

        val fieldsWithNested = listOf(
            Field.of("name", Datum.string("John")),
            Field.of("property", datumWithMixedType)
        )
        val datum = Datum.struct(fields)
        val datumWithNested = Datum.struct(fieldsWithNested)

        assertEquals("DatumStruct{_type=STRUCT, _value={name: [DatumString{_type=STRING, _value=John}], age: [DatumInt{_type=INTEGER, _value=30}]}}", datum.toString())
        assertEquals(
            "DatumStruct{_type=STRUCT, _value={name: [DatumString{_type=STRING, _value=John}], " +
                "property: [DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}]}}",
            datumWithNested.toString()
        )
    }

    @Test
    fun testDatumRowToString() {
        val fields = listOf(
            Field.of("col1", Datum.string("John")),
            Field.of("col2", Datum.integer(30))
        )
        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))

        val fieldsWithNested = listOf(
            Field.of("col1", Datum.string("John")),
            Field.of("col2", datumWithMixedType)
        )
        val datum = Datum.row(fields)
        val datumWithNested = Datum.row(fieldsWithNested)

        assertEquals("DatumRow{_type=ROW(col1: STRING, col2: INTEGER), _value={col2: [DatumInt{_type=INTEGER, _value=30}], col1: [DatumString{_type=STRING, _value=John}]}}", datum.toString())
        assertEquals(
            "DatumRow{_type=ROW(col1: STRING, col2: BAG(DYNAMIC)), " +
<<<<<<< HEAD
                "_value={ col2: [DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}], " +
                "col1: [DatumString{_type=STRING, _value=John}] }",
=======
                "_value={col2: [DatumCollection{_type=BAG(DYNAMIC), _value=[DatumInt{_type=INTEGER, _value=1}, DatumString{_type=STRING, _value=hello world}]}], " +
                "col1: [DatumString{_type=STRING, _value=John}]}}",
>>>>>>> 075f2eb71 (remove extra space)
            datumWithNested.toString()
        )
    }

    @Test
    fun testDatumNullToString() {
        val datum = Datum.nullValue()

        assertEquals("DatumNull{_type=UNKNOWN}", datum.toString())
    }

    @Test
    fun testDatumNullWithTypeToString() {
        val datum = Datum.nullValue(PType.string())

        assertEquals("DatumNull{_type=STRING}", datum.toString())
    }

    @Test
    fun testDatumMissingToString() {
        val datum = Datum.missing()

        assertEquals("DatumMissing{_type=UNKNOWN}", datum.toString())
    }

    @Test
    fun testDatumMissingWithTypeToString() {
        val datum = Datum.missing(PType.integer())

        assertEquals("DatumMissing{_type=INTEGER}", datum.toString())
    }
}
