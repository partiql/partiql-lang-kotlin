package org.partiql.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField

/**
 * Tests for [PType.toJson] and [PType.fromJson] round-trip serialization.
 */
class PTypeJsonTest {

    companion object {
        @JvmStatic
        fun primitiveCases() = listOf(
            PType.dynamic(),
            PType.bool(),
            PType.tinyint(),
            PType.smallint(),
            PType.integer(),
            PType.bigint(),
            PType.real(),
            PType.doublePrecision(),
            PType.string(),
            PType.date(),
            PType.unknown(),
            PType.struct(),
        )

        @JvmStatic
        fun parameterizedCases() = listOf(
            PType.decimal(10, 2),
            PType.numeric(15, 5),
            PType.character(50),
            PType.varchar(100),
            PType.clob(2048),
            PType.blob(4096),
            PType.time(3),
            PType.timez(9),
            PType.timestamp(0),
            PType.timestampz(6),
        )

        @JvmStatic
        fun defaultParameterCases() = listOf(
            PType.decimal(),
            PType.numeric(),
            PType.character(),
            PType.varchar(),
            PType.clob(),
            PType.blob(),
            PType.time(),
            PType.timez(),
            PType.timestamp(),
            PType.timestampz(),
        )

        @JvmStatic
        fun collectionCases() = listOf(
            PType.array(PType.integer()),
            PType.bag(PType.string()),
            PType.array(PType.decimal(10, 2)),
            PType.bag(PType.array(PType.bool())),
        )

        @JvmStatic
        fun intervalYmCases() = listOf(
            PType.intervalYear(4),
            PType.intervalMonth(2),
            PType.intervalYearMonth(3),
        )

        @JvmStatic
        fun intervalDtCases() = listOf(
            PType.intervalDay(5),
            PType.intervalHour(3),
            PType.intervalMinute(4),
            PType.intervalSecond(5, 6),
            PType.intervalDayHour(3),
            PType.intervalDayMinute(4),
            PType.intervalDaySecond(5, 3),
            PType.intervalHourMinute(2),
            PType.intervalHourSecond(3, 6),
            PType.intervalMinuteSecond(4, 2),
        )
    }

    @ParameterizedTest
    @MethodSource("primitiveCases")
    fun `round-trip primitive types`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(original.code(), restored.code())
    }

    @ParameterizedTest
    @MethodSource("parameterizedCases")
    fun `round-trip parameterized types`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(original.code(), restored.code())
        when (original.code()) {
            PType.DECIMAL, PType.NUMERIC -> {
                assertEquals(original.precision, restored.precision)
                assertEquals(original.scale, restored.scale)
            }
            PType.CHAR, PType.VARCHAR, PType.CLOB, PType.BLOB -> {
                assertEquals(original.length, restored.length)
            }
            PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ -> {
                assertEquals(original.precision, restored.precision)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("defaultParameterCases")
    fun `round-trip default parameter types preserve metas`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(original.code(), restored.code())
        assertEquals(original.metas, restored.metas)
    }

    @ParameterizedTest
    @MethodSource("collectionCases")
    fun `round-trip collection types`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(original.code(), restored.code())
        assertEquals(original.typeParameter.code(), restored.typeParameter.code())
    }

    @ParameterizedTest
    @MethodSource("intervalYmCases")
    fun `round-trip INTERVAL_YM types`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.INTERVAL_YM, restored.code())
        assertEquals(original.intervalCode, restored.intervalCode)
        assertEquals(original.precision, restored.precision)
        assertEquals(original.metas, restored.metas)
    }

    @ParameterizedTest
    @MethodSource("intervalDtCases")
    fun `round-trip INTERVAL_DT types`(original: PType) {
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.INTERVAL_DT, restored.code())
        assertEquals(original.intervalCode, restored.intervalCode)
        assertEquals(original.precision, restored.precision)
        assertEquals(original.fractionalPrecision, restored.fractionalPrecision)
        assertEquals(original.metas, restored.metas)
    }

    @Test
    fun `round-trip nested array in bag`() {
        val original = PType.bag(PType.array(PType.integer()))
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.BAG, restored.code())
        assertEquals(PType.ARRAY, restored.typeParameter.code())
        assertEquals(PType.INTEGER, restored.typeParameter.typeParameter.code())
    }

    @Test
    fun `round-trip ROW type`() {
        val original = PType.row(
            PTypeField.of("id", PType.integer()),
            PTypeField.of("name", PType.string()),
            PTypeField.of("score", PType.decimal(10, 2)),
        )
        val json = original.toJson()
        val restored = PType.fromJson(json)
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
    fun `round-trip ROW with nested collection`() {
        val original = PType.row(
            PTypeField.of("tags", PType.array(PType.string())),
            PTypeField.of("active", PType.bool()),
        )
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.ROW, restored.code())
        val fields = restored.fields.toList()
        assertEquals(2, fields.size)
        assertEquals("tags", fields[0].name)
        assertEquals(PType.ARRAY, fields[0].type.code())
        assertEquals(PType.STRING, fields[0].type.typeParameter.code())
    }

    @Test
    fun `round-trip VARIANT type`() {
        val original = PType.variant("ion")
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.VARIANT, restored.code())
    }

    @Test
    fun `INTERVAL_YM JSON contains intervalCode and precision`() {
        val original = PType.intervalYearMonth(5)
        val json = original.toJson()
        assert(json.contains("\"intervalCode\""))
        assert(json.contains("\"YEAR_MONTH\""))
        assert(json.contains("\"precision\""))
    }

    @Test
    fun `INTERVAL_DT JSON contains intervalCode precision and fractionalPrecision`() {
        val original = PType.intervalDaySecond(4, 6)
        val json = original.toJson()
        assert(json.contains("\"intervalCode\""))
        assert(json.contains("\"DAY_SECOND\""))
        assert(json.contains("\"precision\""))
        assert(json.contains("\"fractionalPrecision\""))
    }

    @Test
    fun `INTERVAL_DT with custom metas round-trips`() {
        val original = PType.intervalSecond(5, 3)
        original.metas["custom_key"] = "custom_value"
        val json = original.toJson()
        val restored = PType.fromJson(json)
        assertEquals(PType.INTERVAL_DT, restored.code())
        assertEquals(IntervalCode.SECOND, restored.intervalCode)
        assertEquals(5, restored.precision)
        assertEquals(3, restored.fractionalPrecision)
        assertEquals("custom_value", restored.metas["custom_key"])
    }

    @Test
    fun `fromJson with BOOLEAN alias`() {
        val json = """{"type": "BOOLEAN"}"""
        val restored = PType.fromJson(json)
        assertEquals(PType.BOOL, restored.code())
    }

    @Test
    fun `fromJson with INT alias`() {
        val json = """{"type": "INT"}"""
        val restored = PType.fromJson(json)
        assertEquals(PType.INTEGER, restored.code())
    }

    @Test
    fun `fromJson with DOUBLE PRECISION alias`() {
        val json = """{"type": "DOUBLE PRECISION"}"""
        val restored = PType.fromJson(json)
        assertEquals(PType.DOUBLE, restored.code())
    }

    @Test
    fun `fromJson rejects null node`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("null")
        }
    }

    @Test
    fun `fromJson rejects unsupported type`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "FAKETYPE"}""")
        }
    }

    @Test
    fun `fromJson rejects ROW without fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "ROW"}""")
        }
    }

    @Test
    fun `fromJson rejects ARRAY without element`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "ARRAY"}""")
        }
    }

    @Test
    fun `fromJson rejects DECIMAL without precision`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "DECIMAL"}""")
        }
    }

    @Test
    fun `fromJson BLOB without length returns default`() {
        val restored = PType.fromJson("""{"type": "BLOB"}""")
        assertEquals(PType.BLOB, restored.code())
    }

    @Test
    fun `fromJson rejects INTERVAL_YM without intervalCode`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "INTERVAL_YM", "precision": 4}""")
        }
    }

    @Test
    fun `fromJson rejects INTERVAL_DT without intervalCode`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "INTERVAL_DT", "precision": 4, "fractionalPrecision": 0}""")
        }
    }

    @Test
    fun `fromJson rejects INTERVAL_YM with invalid intervalCode`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "INTERVAL_YM", "intervalCode": "DAY", "precision": 4}""")
        }
    }

    @Test
    fun `fromJson rejects INTERVAL_DT with invalid intervalCode`() {
        assertThrows(IllegalArgumentException::class.java) {
            PType.fromJson("""{"type": "INTERVAL_DT", "intervalCode": "YEAR", "precision": 4, "fractionalPrecision": 0}""")
        }
    }
}
