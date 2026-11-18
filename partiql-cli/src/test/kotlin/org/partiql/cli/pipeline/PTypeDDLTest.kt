package org.partiql.cli.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField

/**
 * Tests for [PTypeSerde.toDDL] and [PTypeSerde.fromDDL] round-trip serialization.
 */
class PTypeDDLTest {

    data class DDLCase(val pType: PType, val expectedDDL: String)

    companion object {
        @JvmStatic
        fun simpleCases() = listOf(
            DDLCase(PType.dynamic(), "DYNAMIC"),
            DDLCase(PType.bool(), "BOOL"),
            DDLCase(PType.tinyint(), "TINYINT"),
            DDLCase(PType.smallint(), "SMALLINT"),
            DDLCase(PType.integer(), "INTEGER"),
            DDLCase(PType.bigint(), "BIGINT"),
            DDLCase(PType.real(), "REAL"),
            DDLCase(PType.doublePrecision(), "DOUBLE PRECISION"),
            DDLCase(PType.string(), "STRING"),
            DDLCase(PType.date(), "DATE"),
            DDLCase(PType.struct(), "STRUCT"),
            DDLCase(PType.unknown(), "UNKNOWN"),
            DDLCase(PType.variant("ion"), "VARIANT"),
        )

        @JvmStatic
        fun parameterizedCases() = listOf(
            DDLCase(PType.decimal(10, 2), "DECIMAL(10, 2)"),
            DDLCase(PType.numeric(15, 5), "NUMERIC(15, 5)"),
            DDLCase(PType.character(50), "CHAR(50)"),
            DDLCase(PType.varchar(100), "VARCHAR(100)"),
            DDLCase(PType.blob(4096), "BLOB(4096)"),
            DDLCase(PType.clob(2048), "CLOB(2048)"),
            DDLCase(PType.time(3), "TIME(3)"),
            DDLCase(PType.timez(9), "TIME(9) WITH TIME ZONE"),
            DDLCase(PType.timestamp(0), "TIMESTAMP(0)"),
            DDLCase(PType.timestampz(6), "TIMESTAMP(6) WITH TIME ZONE"),
        )

        @JvmStatic
        fun collectionCases() = listOf(
            DDLCase(PType.array(PType.integer()), "ARRAY<INTEGER>"),
            DDLCase(PType.bag(PType.string()), "BAG<STRING>"),
            DDLCase(PType.array(PType.decimal(10, 2)), "ARRAY<DECIMAL(10, 2)>"),
            DDLCase(PType.bag(PType.array(PType.bool())), "BAG<ARRAY<BOOL>>"),
        )

        @JvmStatic
        fun intervalYmCases() = listOf(
            DDLCase(PType.intervalYear(4), "INTERVAL YEAR(4)"),
            DDLCase(PType.intervalMonth(2), "INTERVAL MONTH(2)"),
            DDLCase(PType.intervalYearMonth(3), "INTERVAL YEAR(3) TO MONTH"),
        )

        @JvmStatic
        fun intervalDtCases() = listOf(
            DDLCase(PType.intervalDay(5), "INTERVAL DAY(5)"),
            DDLCase(PType.intervalHour(3), "INTERVAL HOUR(3)"),
            DDLCase(PType.intervalMinute(4), "INTERVAL MINUTE(4)"),
            DDLCase(PType.intervalSecond(5, 6), "INTERVAL SECOND(5, 6)"),
            DDLCase(PType.intervalDayHour(3), "INTERVAL DAY(3) TO HOUR"),
            DDLCase(PType.intervalDayMinute(4), "INTERVAL DAY(4) TO MINUTE"),
            DDLCase(PType.intervalDaySecond(5, 3), "INTERVAL DAY(5) TO SECOND(3)"),
            DDLCase(PType.intervalHourMinute(2), "INTERVAL HOUR(2) TO MINUTE"),
            DDLCase(PType.intervalHourSecond(3, 6), "INTERVAL HOUR(3) TO SECOND(6)"),
            DDLCase(PType.intervalMinuteSecond(4, 2), "INTERVAL MINUTE(4) TO SECOND(2)"),
        )
    }

    // -----------------------------------------------
    // toDDL tests
    // -----------------------------------------------

    @ParameterizedTest
    @MethodSource("simpleCases")
    fun `toDDL simple types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(case.pType))
    }

    @ParameterizedTest
    @MethodSource("parameterizedCases")
    fun `toDDL parameterized types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(case.pType))
    }

    @ParameterizedTest
    @MethodSource("collectionCases")
    fun `toDDL collection types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(case.pType))
    }

    @ParameterizedTest
    @MethodSource("intervalYmCases")
    fun `toDDL INTERVAL_YM types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(case.pType))
    }

    @ParameterizedTest
    @MethodSource("intervalDtCases")
    fun `toDDL INTERVAL_DT types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(case.pType))
    }

    @Test
    fun `toDDL ROW type`() {
        val row = PType.row(
            PTypeField.of("id", PType.integer()),
            PTypeField.of("name", PType.string()),
            PTypeField.of("score", PType.decimal(10, 2)),
        )
        assertEquals("ROW(id INTEGER, name STRING, score DECIMAL(10, 2))", PTypeSerde.toDDL(row))
    }

    @Test
    fun `toDDL ROW with nested collection`() {
        val row = PType.row(
            PTypeField.of("tags", PType.array(PType.string())),
            PTypeField.of("active", PType.bool()),
        )
        assertEquals("ROW(tags ARRAY<STRING>, active BOOL)", PTypeSerde.toDDL(row))
    }

    // -----------------------------------------------
    // fromDDL tests
    // -----------------------------------------------

    @ParameterizedTest
    @MethodSource("simpleCases")
    fun `fromDDL simple types`(case: DDLCase) {
        val restored = PTypeSerde.fromDDL(case.expectedDDL)
        assertEquals(case.pType.code(), restored.code())
    }

    @ParameterizedTest
    @MethodSource("parameterizedCases")
    fun `fromDDL parameterized types`(case: DDLCase) {
        val restored = PTypeSerde.fromDDL(case.expectedDDL)
        assertEquals(case.pType.code(), restored.code())
        when (case.pType.code()) {
            PType.DECIMAL, PType.NUMERIC -> {
                assertEquals(case.pType.precision, restored.precision)
                assertEquals(case.pType.scale, restored.scale)
            }
            PType.CHAR, PType.VARCHAR, PType.CLOB, PType.BLOB -> {
                assertEquals(case.pType.length, restored.length)
            }
            PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ -> {
                assertEquals(case.pType.precision, restored.precision)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("collectionCases")
    fun `fromDDL collection types`(case: DDLCase) {
        val restored = PTypeSerde.fromDDL(case.expectedDDL)
        assertEquals(case.pType.code(), restored.code())
        assertEquals(case.pType.typeParameter.code(), restored.typeParameter.code())
    }

    @ParameterizedTest
    @MethodSource("intervalYmCases")
    fun `fromDDL INTERVAL_YM types`(case: DDLCase) {
        val restored = PTypeSerde.fromDDL(case.expectedDDL)
        assertEquals(PType.INTERVAL_YM, restored.code())
        assertEquals(case.pType.intervalCode, restored.intervalCode)
        assertEquals(case.pType.precision, restored.precision)
    }

    @ParameterizedTest
    @MethodSource("intervalDtCases")
    fun `fromDDL INTERVAL_DT types`(case: DDLCase) {
        val restored = PTypeSerde.fromDDL(case.expectedDDL)
        assertEquals(PType.INTERVAL_DT, restored.code())
        assertEquals(case.pType.intervalCode, restored.intervalCode)
        assertEquals(case.pType.precision, restored.precision)
        assertEquals(case.pType.fractionalPrecision, restored.fractionalPrecision)
    }

    @Test
    fun `fromDDL ROW type`() {
        val restored = PTypeSerde.fromDDL("ROW(id INTEGER, name STRING, score DECIMAL(10, 2))")
        assertEquals(PType.ROW, restored.code())
        val fields = restored.fields.toList()
        assertEquals(3, fields.size)
        assertEquals("id", fields[0].name)
        assertEquals(PType.INTEGER, fields[0].type.code())
        assertEquals("name", fields[1].name)
        assertEquals(PType.STRING, fields[1].type.code())
        assertEquals("score", fields[2].name)
        assertEquals(PType.DECIMAL, fields[2].type.code())
        assertEquals(10, fields[2].type.precision)
        assertEquals(2, fields[2].type.scale)
    }

    @Test
    fun `fromDDL ROW with nested collection`() {
        val restored = PTypeSerde.fromDDL("ROW(tags ARRAY<STRING>, active BOOL)")
        assertEquals(PType.ROW, restored.code())
        val fields = restored.fields.toList()
        assertEquals(2, fields.size)
        assertEquals("tags", fields[0].name)
        assertEquals(PType.ARRAY, fields[0].type.code())
        assertEquals(PType.STRING, fields[0].type.typeParameter.code())
        assertEquals("active", fields[1].name)
        assertEquals(PType.BOOL, fields[1].type.code())
    }

    // -----------------------------------------------
    // round-trip tests
    // -----------------------------------------------

    @ParameterizedTest
    @MethodSource("simpleCases")
    fun `round-trip simple types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(PTypeSerde.fromDDL(case.expectedDDL)))
    }

    @ParameterizedTest
    @MethodSource("parameterizedCases")
    fun `round-trip parameterized types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(PTypeSerde.fromDDL(case.expectedDDL)))
    }

    @ParameterizedTest
    @MethodSource("collectionCases")
    fun `round-trip collection types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(PTypeSerde.fromDDL(case.expectedDDL)))
    }

    @ParameterizedTest
    @MethodSource("intervalYmCases")
    fun `round-trip INTERVAL_YM types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(PTypeSerde.fromDDL(case.expectedDDL)))
    }

    @ParameterizedTest
    @MethodSource("intervalDtCases")
    fun `round-trip INTERVAL_DT types`(case: DDLCase) {
        assertEquals(case.expectedDDL, PTypeSerde.toDDL(PTypeSerde.fromDDL(case.expectedDDL)))
    }

    @Test
    fun `round-trip ROW`() {
        val ddl = "ROW(id INTEGER, name STRING, score DECIMAL(10, 2))"
        assertEquals(ddl, PTypeSerde.toDDL(PTypeSerde.fromDDL(ddl)))
    }

    @Test
    fun `round-trip nested ROW in ARRAY`() {
        val ddl = "ARRAY<ROW(x INTEGER, y STRING)>"
        assertEquals(ddl, PTypeSerde.toDDL(PTypeSerde.fromDDL(ddl)))
    }

    // -----------------------------------------------
    // fromDDL aliases
    // -----------------------------------------------

    @Test
    fun `fromDDL BOOLEAN alias`() {
        assertEquals(PType.BOOL, PTypeSerde.fromDDL("BOOLEAN").code())
    }

    @Test
    fun `fromDDL INT alias`() {
        assertEquals(PType.INTEGER, PTypeSerde.fromDDL("INT").code())
    }

    @Test
    fun `fromDDL bare DECIMAL defaults to 38 0`() {
        val restored = PTypeSerde.fromDDL("DECIMAL")
        assertEquals(PType.DECIMAL, restored.code())
        assertEquals(38, restored.precision)
        assertEquals(0, restored.scale)
    }

    @Test
    fun `fromDDL bare NUMERIC defaults to 38 0`() {
        val restored = PTypeSerde.fromDDL("NUMERIC")
        assertEquals(PType.NUMERIC, restored.code())
        assertEquals(38, restored.precision)
        assertEquals(0, restored.scale)
    }

    @Test
    fun `fromDDL bare TIME defaults to precision 6`() {
        val restored = PTypeSerde.fromDDL("TIME")
        assertEquals(PType.TIME, restored.code())
        assertEquals(6, restored.precision)
    }

    @Test
    fun `fromDDL bare TIMESTAMP defaults to precision 6`() {
        val restored = PTypeSerde.fromDDL("TIMESTAMP")
        assertEquals(PType.TIMESTAMP, restored.code())
        assertEquals(6, restored.precision)
    }

    // -----------------------------------------------
    // error cases
    // -----------------------------------------------

    @Test
    fun `fromDDL rejects empty string`() {
        assertThrows(IllegalArgumentException::class.java) {
            PTypeSerde.fromDDL("")
        }
    }

    @Test
    fun `fromDDL rejects unknown type`() {
        assertThrows(IllegalArgumentException::class.java) {
            PTypeSerde.fromDDL("FAKETYPE")
        }
    }

    @Test
    fun `fromDDL rejects DECIMAL with wrong arity`() {
        assertThrows(IllegalArgumentException::class.java) {
            PTypeSerde.fromDDL("DECIMAL(10)")
        }
    }

    @Test
    fun `fromDDL rejects ROW field without type`() {
        assertThrows(IllegalArgumentException::class.java) {
            PTypeSerde.fromDDL("ROW(id)")
        }
    }
}
