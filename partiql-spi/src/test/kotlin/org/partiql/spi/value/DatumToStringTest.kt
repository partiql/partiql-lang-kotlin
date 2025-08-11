package org.partiql.spi.value

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

/**
 * Test cases for toString methods of all Datum implementations.
 * Verifies that each Datum class produces expected string representations.
 *
 * Test methods are in alphabetical order by method name
 */
@ExtendWith(SnapshotExtension::class)
class DatumToStringTest {

    private lateinit var expect: Expect

    @Test
    fun testDatumBagToString() {
        val datumWithSingleType = Datum.bag(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)))
        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))
        val datumWithNestedType = Datum.bagVararg(Datum.integer(1), datumWithSingleType, datumWithMixedType)

        expect.scenario("datumWithSingleType").toMatchSnapshot(datumWithSingleType)
        expect.scenario("datumWithMixedType").toMatchSnapshot(datumWithMixedType)
        expect.scenario("datumWithNestedType").toMatchSnapshot(datumWithNestedType)
    }

    @Test
    fun testDatumBooleanToString() {
        val datumTrue = Datum.bool(true)
        val datumFalse = Datum.bool(false)

        expect.scenario("datumTrue").toMatchSnapshot(datumTrue)
        expect.scenario("datumFalse").toMatchSnapshot(datumFalse)
    }

    @Test
    fun testDatumByteToString() {
        val datum = Datum.tinyint(42.toByte())

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumBytesToString() {
        val datum = Datum.blob(byteArrayOf(10, 50, 100, 127))

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumCharsToString() {
        val datum = Datum.character("test", 10)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumCollectionToString() {
        val datumWithSingleType = Datum.array(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)))
        val datumWithMixedType = Datum.array(listOf(Datum.integer(1), Datum.string("hello world")))
        val datumWithNestedType = Datum.array(listOf(Datum.integer(1), datumWithSingleType, datumWithMixedType))

        expect.scenario("datumWithSingleType").toMatchSnapshot(datumWithSingleType)
        expect.scenario("datumWithMixedType").toMatchSnapshot(datumWithMixedType)
        expect.scenario("datumWithNestedType").toMatchSnapshot(datumWithNestedType)
    }

    @Test
    fun testDatumDateToString() {
        val datum = Datum.date(LocalDate.of(2023, 12, 25))

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumDecimalToString() {
        val datum = Datum.decimal(BigDecimal("123.45"), 5, 2)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumDoubleToString() {
        val datum = Datum.doublePrecision(2.718)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumFloatToString() {
        val datum = Datum.real(3.14f)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumIntToString() {
        val datum = Datum.integer(42)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumIntervalDayTimeToString() {
        val datumDaySecond = Datum.intervalDaySecond(1, 2, 30, 45, 500000000, 2, 6)

        expect.scenario("datumDaySecond").toMatchSnapshot(datumDaySecond)
    }

    @Test
    fun testDatumIntervalYearMonthToString() {
        val datumYearMonth = Datum.intervalYearMonth(2, 6, 2)

        expect.scenario("datumYearMonth").toMatchSnapshot(datumYearMonth)
    }

    @Test
    fun testDatumLongToString() {
        val datum = Datum.bigint(123456789L)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumMissingToString() {
        val datum = Datum.missing()

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumMissingWithTypeToString() {
        val datum = Datum.missing(PType.integer())

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumNullToString() {
        val datum = Datum.nullValue()

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumNullWithTypeToString() {
        val datum = Datum.nullValue(PType.string())

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumRowToString() {
        val fields = listOf(
            Field.of("col1", Datum.string("John")),
            Field.of("col2", Datum.integer(30))
        )
        val fieldsWithDuplicateKey = listOf(
            Field.of("col1", Datum.string("John")),
            Field.of("col1", Datum.integer(30))
        )

        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))

        val fieldsWithNested = listOf(
            Field.of("col1", Datum.string("John")),
            Field.of("col2", datumWithMixedType)
        )

        val datum = Datum.row(fields)
        val datumWithNested = Datum.row(fieldsWithNested)
        val datumWithDuplicateKey = Datum.row(fieldsWithDuplicateKey)

        expect.scenario("datum").toMatchSnapshot(datum)
        expect.scenario("datumWithNested").toMatchSnapshot(datumWithNested)
        expect.scenario("datumWithDuplicateKey").toMatchSnapshot(datumWithDuplicateKey)
    }

    @Test
    fun testDatumShortToString() {
        val datum = Datum.smallint(1234.toShort())

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumStringToString() {
        val datum = Datum.string("hello world")

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumStructToString() {
        val fields = listOf(
            Field.of("name", Datum.string("John")),
            Field.of("age", Datum.integer(30))
        )
        val fieldsWithDuplicateKey = listOf(
            Field.of("name", Datum.string("John")),
            Field.of("name", Datum.integer(30))
        )

        val datumWithMixedType = Datum.bagVararg(Datum.integer(1), Datum.string("hello world"))

        val fieldsWithNested = listOf(
            Field.of("name", Datum.string("John")),
            Field.of("property", datumWithMixedType)
        )

        val datum = Datum.struct(fields)
        val datumWithNested = Datum.struct(fieldsWithNested)
        val datumWithDuplicateKey = Datum.struct(fieldsWithDuplicateKey)

        expect.scenario("datum").toMatchSnapshot(datum)
        expect.scenario("datumWithNested").toMatchSnapshot(datumWithNested)
        expect.scenario("datumWithDuplicateKey").toMatchSnapshot(datumWithDuplicateKey)
    }

    @Test
    fun testDatumTimeToString() {
        val datum = Datum.time(LocalTime.of(14, 30, 45), 0)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumTimestampToString() {
        val datum = Datum.timestamp(LocalDateTime.of(2023, 12, 25, 14, 30, 45), 0)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumTimestampzToString() {
        val datum = Datum.timestampz(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.UTC), 0)

        expect.scenario("datum").toMatchSnapshot(datum)
    }

    @Test
    fun testDatumTimezToString() {
        val datum = Datum.timez(OffsetTime.of(14, 30, 45, 0, ZoneOffset.UTC), 0)

        expect.scenario("datum").toMatchSnapshot(datum)
    }
}
