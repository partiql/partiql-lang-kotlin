package org.partiql.cli.io

import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class DatumWriterTextPrettyTests {
    companion object {
        val EXPECTED_OUTPUT = """
        <<
          null,
          null,
          missing,
          missing,
          [],
          <<>>,
          <<
            <<>>
          >>,
          [
            []
          ],
          <<
            5,
            <<>>
          >>,
          [
            5,
            []
          ],
          true,
          false,
          1,
          2,
          3,
          4,
          5.0,
          7,
          TIMESTAMP '2025-01-01T05:02',
          TIMESTAMP '2025-01-01T05:02+08:00',
          DATE '2025-01-01',
          TIME '05:02',
          TIME '05:02+08:00',
          '11',
          BLOB '13',
          INTERVAL '5' YEAR (2),
          INTERVAL '3' MONTH (1),
          INTERVAL '1' DAY (2),
          INTERVAL '1' HOUR (2),
          INTERVAL '1' MINUTE (2),
          INTERVAL '1.10' SECOND (2, 2),
          INTERVAL '5-2' YEAR (2) TO MONTH,
          INTERVAL '1 1' DAY (2) TO HOUR,
          INTERVAL '1 2:3' DAY (2) TO MINUTE,
          INTERVAL '1 2:3:4.50' DAY (2) TO SECOND (2),
          INTERVAL '1:2' HOUR (2) TO MINUTE,
          INTERVAL '1:2:3.40' HOUR (2) TO SECOND (2),
          INTERVAL '1:2.30' MINUTE (2) TO SECOND (2),
          {
            'bar': [
              1,
              2
            ],
            'foo': 3.14,
            'baz': <<
              [
                {
                  'a': 'deeply nested'
                }
              ]
            >>
          }
        >>
        """.trimIndent()

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(5, 2)
        val tzOffset = ZoneOffset.ofHours(8)
        val offsetTime = OffsetTime.of(localTime, tzOffset)
        val offsetDateTime = OffsetDateTime.of(localDate, localTime, tzOffset)
        val data = Datum.bagVararg(
            Datum.nullValue(PType.real()), // null
            Datum.nullValue(PType.doublePrecision()), // null
            Datum.missing(PType.real()), // missing
            Datum.missing(PType.doublePrecision()), // missing
            Datum.array(emptyList()), // []
            Datum.bagVararg(), // << >>
            Datum.bagVararg(Datum.bagVararg()), // << << >> >>
            Datum.array(listOf(Datum.array(emptyList()))), // [ [] ]
            Datum.bagVararg(Datum.integer(5), Datum.bagVararg()), // << 5, << >> >>
            Datum.array(listOf(Datum.integer(5), Datum.array(emptyList()))), // [ 5, [ ] ]
            Datum.bool(true),
            Datum.bool(false),
            Datum.tinyint(1),
            Datum.smallint(2),
            Datum.integer(3),
            Datum.bigint(4),
            Datum.real(5.0f),
            Datum.decimal(BigDecimal(7)),
            Datum.timestamp(LocalDateTime.of(localDate, localTime), 4),
            Datum.timestampz(offsetDateTime, 4),
            Datum.date(localDate), // 2025-01-01
            Datum.time(localTime, 2), // 05:02
            Datum.timez(offsetTime, 2), // 05:02+08:00
            Datum.string("11"),
            Datum.blob("13".toByteArray()),
            Datum.intervalYear(5, 2),
            Datum.intervalMonth(3, 1),
            Datum.intervalDay(1, 2),
            Datum.intervalHour(1, 2),
            Datum.intervalMinute(1, 2),
            Datum.intervalSecond(1, 100000000, 2, 2),
            Datum.intervalYearMonth(5, 2, 2),
            Datum.intervalDayHour(1, 1, 2),
            Datum.intervalDayMinute(1, 2, 3, 2),
            Datum.intervalDaySecond(1, 2, 3, 4, 500000000, 2, 2),
            Datum.intervalHourMinute(1, 2, 2),
            Datum.intervalHourSecond(1, 2, 3, 400000000, 2, 2),
            Datum.intervalMinuteSecond(1, 2, 300000000, 2, 2),
            // TODO: Technically, structs and bags are unordered. This may or may not lead to issues in the future
            //  when testing. We should potentially instead rewrite these tests to re-parse the output and do a full-on
            //  comparison between the values instead. This approach, however, would not take into account the indents.
            //  For now, leaving this in for this reason.
            Datum.struct(
                Field.of("foo", Datum.doublePrecision(3.14)),
                Field.of("bar", Datum.array(listOf(Datum.integer(1), Datum.integer(2)))),
                Field.of(
                    "baz",
                    Datum.bagVararg(
                        Datum.array(
                            listOf(
                                Datum.struct(Field.of("a", Datum.string("deeply nested")))
                            )
                        )
                    )
                ),
            ),
        )
    }

    @Test
    fun test() {
        // Prepare
        val sb: StringBuilder = StringBuilder()
        val writer = DatumWriterTextPretty(sb)
        // Test
        writer.write(data)
        // Assert
        assertEquals(EXPECTED_OUTPUT, sb.toString())
    }
}
