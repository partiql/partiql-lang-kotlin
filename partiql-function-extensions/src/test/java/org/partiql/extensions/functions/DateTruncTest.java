/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.extensions.functions;

import org.junit.jupiter.api.Test;
import org.partiql.extensions.functions.custom.datetime.DateTrunc;
import org.partiql.spi.function.Fn;
import org.partiql.spi.types.PType;
import org.partiql.spi.value.Datum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateTruncTest {
    private static final Fn TIMESTAMP_FUNCTION = FunctionTestSupport.function(
        FunctionTestSupport.overloadFor(DateTrunc.overloads(), PType.string(), PType.timestamp())
    );
    private static final Fn TIMESTAMPZ_FUNCTION = FunctionTestSupport.function(
        FunctionTestSupport.overloadFor(DateTrunc.overloads(), PType.string(), PType.timestampz())
    );
    private static final Fn DATE_FUNCTION = FunctionTestSupport.function(
        FunctionTestSupport.overloadFor(DateTrunc.overloads(), PType.string(), PType.date())
    );

    // Thursday
    private static final LocalDateTime BASE = LocalDateTime.of(2020, 8, 13, 10, 20, 30, 123_456_789);

    @Test
    public void truncatesTimestampToEveryPart() {
        assertTimestamp("millisecond", LocalDateTime.of(2020, 8, 13, 10, 20, 30, 123_000_000));
        assertTimestamp("second", LocalDateTime.of(2020, 8, 13, 10, 20, 30));
        assertTimestamp("minute", LocalDateTime.of(2020, 8, 13, 10, 20));
        assertTimestamp("hour", LocalDateTime.of(2020, 8, 13, 10, 0));
        assertTimestamp("day", LocalDateTime.of(2020, 8, 13, 0, 0));
        assertTimestamp("week", LocalDateTime.of(2020, 8, 10, 0, 0));
        assertTimestamp("month", LocalDateTime.of(2020, 8, 1, 0, 0));
        assertTimestamp("quarter", LocalDateTime.of(2020, 7, 1, 0, 0));
        assertTimestamp("year", LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    @Test
    public void acceptsCaseInsensitiveParts() {
        assertTimestamp("MONTH", LocalDateTime.of(2020, 8, 1, 0, 0));
    }

    @Test
    public void weekStartsOnMondayAcrossMonthAndYearBoundaries() {
        // Sunday, 2021-01-03 -> Monday, 2020-12-28
        Datum result = TIMESTAMP_FUNCTION.invoke(new Datum[] {
            Datum.string("week"),
            Datum.timestamp(LocalDateTime.of(2021, 1, 3, 23, 59), 3)
        });

        assertEquals(LocalDateTime.of(2020, 12, 28, 0, 0), result.getLocalDateTime());
    }

    @Test
    public void truncatesTimestampWithTimeZoneInItsOwnOffset() {
        // Like PostgreSQL with the offset as the session time zone: 2020-08-13 23:20:30-05 -> 2020-08-13 00:00:00-05.
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        for (int precision : new int[] {0, 6}) {
            Datum result = TIMESTAMPZ_FUNCTION.invoke(new Datum[] {
                Datum.string("day"),
                Datum.timestampz(OffsetDateTime.of(2020, 8, 13, 23, 20, 30, 0, offset), precision)
            });

            assertEquals(PType.timestampz(precision), result.getType());
            assertEquals(OffsetDateTime.of(2020, 8, 13, 0, 0, 0, 0, offset), result.getOffsetDateTime());
        }
        Datum week = TIMESTAMPZ_FUNCTION.invoke(new Datum[] {
            Datum.string("week"),
            Datum.timestampz(OffsetDateTime.of(2021, 1, 3, 23, 59, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), 6)
        });
        assertEquals(OffsetDateTime.of(2020, 12, 28, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), week.getOffsetDateTime());
    }

    @Test
    public void truncatesDateToTimestamp() {
        Datum result = DATE_FUNCTION.invoke(new Datum[] {
            Datum.string("quarter"),
            Datum.date(LocalDate.of(2020, 12, 31))
        });

        assertEquals(PType.timestamp(), result.getType());
        assertEquals(LocalDateTime.of(2020, 10, 1, 0, 0), result.getLocalDateTime());
    }

    @Test
    public void quarterStartsInJanuaryAprilJulyAndOctober() {
        for (int month = 1; month <= 12; month++) {
            LocalDateTime lastMoment = LocalDateTime.of(2021, month, 1, 23, 59, 59, 999_000_000)
                .withDayOfMonth(YearMonth.of(2021, month).lengthOfMonth());
            int firstMonth = ((month - 1) / 3) * 3 + 1;

            assertEquals(LocalDateTime.of(2021, firstMonth, 1, 0, 0), truncate("quarter", lastMoment), "month " + month);
        }
    }

    @Test
    public void valuesAlreadyOnABoundaryAreUnchanged() {
        LocalDateTime monday = LocalDateTime.of(2020, 8, 10, 0, 0);
        LocalDateTime newYear = LocalDateTime.of(2020, 1, 1, 0, 0);

        assertEquals(monday, truncate("week", monday));
        assertEquals(monday, truncate("second", monday));
        assertEquals(newYear, truncate("year", newYear));
        assertEquals(newYear, truncate("quarter", newYear));
    }

    @Test
    public void handlesLeapDay() {
        LocalDateTime leapDay = LocalDateTime.of(2020, 2, 29, 23, 59, 59, 999_999_000);

        assertEquals(LocalDateTime.of(2020, 2, 29, 0, 0), truncate("day", leapDay));
        assertEquals(LocalDateTime.of(2020, 2, 1, 0, 0), truncate("month", leapDay));
        assertEquals(LocalDateTime.of(2020, 2, 24, 0, 0), truncate("week", leapDay));
    }

    @Test
    public void preservesTimestampPrecision() {
        for (int precision : new int[] {0, 3, 6}) {
            Datum result = TIMESTAMP_FUNCTION.invoke(new Datum[] {
                Datum.string("hour"),
                Datum.timestamp(BASE, precision)
            });

            assertEquals(PType.timestamp(precision), result.getType());
        }
    }

    @Test
    public void truncatesDateToEveryPart() {
        LocalDate thursday = LocalDate.of(2020, 8, 13);
        LocalDateTime midnight = thursday.atStartOfDay();
        for (String part : new String[] {"millisecond", "second", "minute", "hour", "day"}) {
            assertEquals(midnight, truncateDate(part, thursday), part);
        }
        assertEquals(LocalDateTime.of(2020, 8, 10, 0, 0), truncateDate("week", thursday));
        assertEquals(LocalDateTime.of(2020, 8, 1, 0, 0), truncateDate("month", thursday));
        assertEquals(LocalDateTime.of(2020, 7, 1, 0, 0), truncateDate("quarter", thursday));
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), truncateDate("year", thursday));
    }

    @Test
    public void rejectsUnsupportedPartsForDates() {
        assertThrows(IllegalArgumentException.class, () -> truncateDate("century", LocalDate.of(2020, 8, 13)));
    }

    @Test
    public void rejectsUnsupportedPart() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> TIMESTAMP_FUNCTION.invoke(new Datum[] {
                Datum.string("century"),
                Datum.timestamp(BASE, 3)
            })
        );

        assertEquals("Invalid datetime part for date_trunc: century", error.getMessage());

        for (String part : new String[] {"microsecond", "decade", "millennium", "weeks", " day", ""}) {
            assertThrows(IllegalArgumentException.class, () -> truncate(part, BASE), "'" + part + "'");
        }
    }

    private static LocalDateTime truncate(String part, LocalDateTime value) {
        return TIMESTAMP_FUNCTION.invoke(new Datum[] {
            Datum.string(part),
            Datum.timestamp(value, 6)
        }).getLocalDateTime();
    }

    private static LocalDateTime truncateDate(String part, LocalDate value) {
        return DATE_FUNCTION.invoke(new Datum[] {
            Datum.string(part),
            Datum.date(value)
        }).getLocalDateTime();
    }

    private static void assertTimestamp(String part, LocalDateTime expected) {
        Datum result = TIMESTAMP_FUNCTION.invoke(new Datum[] {
            Datum.string(part),
            Datum.timestamp(BASE, 3)
        });

        assertEquals(PType.timestamp(3), result.getType());
        assertEquals(expected, result.getLocalDateTime());
    }
}
