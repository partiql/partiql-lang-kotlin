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

package org.partiql.extensions.functions.custom.datetime;

import org.partiql.spi.function.FnOverload;
import org.partiql.spi.types.PType;
import org.partiql.spi.value.Datum;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code date_trunc} overloads that truncate a timestamp or date to the start of a datetime part.
 *
 * <p>Supported parts (case-insensitive) are {@code millisecond}, {@code second}, {@code minute}, {@code hour},
 * {@code day}, {@code week}, {@code month}, {@code quarter}, and {@code year}. Weeks start on Monday (ISO 8601).
 * The behavior follows PostgreSQL's {@code date_trunc}: a date argument is treated as midnight of that day, and the
 * result is a timestamp. A {@code TIMESTAMP WITH TIME ZONE} is truncated in its own offset and keeps it, as in
 * PostgreSQL with that offset as the session time zone.
 */
public final class DateTrunc {
    private static final List<FnOverload> OVERLOADS = Collections.unmodifiableList(Arrays.asList(
        new FnOverload.Builder("date_trunc")
            .addParameters(PType.string(), PType.timestamp())
            .returns(PType.timestamp())
            .isNullCall(true)
            .body(args -> Datum.timestamp(
                truncate(args[0].getString(), args[1].getLocalDateTime()),
                args[1].getType().getPrecision()
            ))
            .build(),
        new FnOverload.Builder("date_trunc")
            .addParameters(PType.string(), PType.timestampz())
            .returns(PType.timestampz())
            .isNullCall(true)
            .body(args -> {
                // Truncates the value's own date and time, and keeps its offset.
                OffsetDateTime value = args[1].getOffsetDateTime();
                return Datum.timestampz(
                    truncate(args[0].getString(), value.toLocalDateTime()).atOffset(value.getOffset()),
                    args[1].getType().getPrecision()
                );
            })
            .build(),
        new FnOverload.Builder("date_trunc")
            .addParameters(PType.string(), PType.date())
            .returns(PType.timestamp())
            .isNullCall(true)
            .body(args -> Datum.timestamp(
                truncate(args[0].getString(), args[1].getLocalDate().atStartOfDay()),
                PType.timestamp().getPrecision()
            ))
            .build()
    ));

    private DateTrunc() {
    }

    /**
     * Returns the immutable {@code date_trunc} overloads.
     *
     * @return {@code date_trunc} overloads
     */
    public static List<FnOverload> overloads() {
        return OVERLOADS;
    }

    private static LocalDateTime truncate(String part, LocalDateTime value) {
        switch (part.toLowerCase(Locale.ROOT)) {
            case "millisecond":
                return value.truncatedTo(ChronoUnit.MILLIS);
            case "second":
                return value.truncatedTo(ChronoUnit.SECONDS);
            case "minute":
                return value.truncatedTo(ChronoUnit.MINUTES);
            case "hour":
                return value.truncatedTo(ChronoUnit.HOURS);
            case "day":
                return value.truncatedTo(ChronoUnit.DAYS);
            case "week":
                return value.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month":
                return value.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case "quarter":
                int firstMonthOfQuarter = ((value.getMonthValue() - 1) / 3) * 3 + 1;
                return value.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).withMonth(firstMonthOfQuarter);
            case "year":
                return value.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            default:
                throw new IllegalArgumentException("Invalid datetime part for date_trunc: " + part);
        }
    }
}
