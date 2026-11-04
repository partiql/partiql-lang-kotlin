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
import org.partiql.extensions.functions.custom.datetime.ToChar;
import org.partiql.spi.function.Fn;
import org.partiql.spi.types.PType;
import org.partiql.spi.value.Datum;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ToCharTest {
    private static final Fn TIMESTAMP_FUNCTION = FunctionTestSupport.function(
        FunctionTestSupport.overloadFor(ToChar.overloads(), PType.timestamp(), PType.string())
    );
    private static final Fn DATE_FUNCTION = FunctionTestSupport.function(
        FunctionTestSupport.overloadFor(ToChar.overloads(), PType.date(), PType.string())
    );

    // Thursday, day 226 of a leap year, ISO week 33
    private static final LocalDateTime BASE = LocalDateTime.of(2020, 8, 13, 14, 5, 9, 123_456_000);

    @Test
    public void formatsCommonTemplates() {
        assertFormat(BASE, "YYYY-MM-DD HH24:MI:SS", "2020-08-13 14:05:09");
        assertFormat(BASE, "YYYY-MM-DD\"T\"HH24:MI:SS.MS", "2020-08-13T14:05:09.123");
        assertFormat(BASE, "YYYYMMDD", "20200813");
        assertFormat(BASE, "yyyy-mm-dd hh24:mi:ss", "2020-08-13 14:05:09");
        assertFormat(BASE, "HH12:MI:SS AM", "02:05:09 PM");
        assertFormat(BASE, "hh:mi a.m.", "02:05 p.m.");
    }

    @Test
    public void formatsYearPatterns() {
        assertFormat(BASE, "Y,YYY|YYYY|YYY|YY|Y", "2,020|2020|020|20|0");
        assertFormat(BASE, "RRRR|RR|CC|IYYY|IYY|IY|I", "2020|20|21|2020|020|20|0");
        assertFormat(BASE, "AD|bc|A.D.|b.c.", "AD|ad|A.D.|a.d.");
        assertFormat(LocalDateTime.of(-43, 3, 15, 0, 0), "YYYY BC", "0044 BC");
    }

    @Test
    public void formatsMonthWeekAndDayPatterns() {
        assertFormat(BASE, "MM|Q|RM|WW|W|IW", "08|3|VIII|33|2|33");
        assertFormat(BASE, "DDD|DD|D|IDDD|ID|J", "226|13|5|228|4|2459075");
        assertFormat(LocalDateTime.of(2020, 1, 5, 0, 0), "rm|FMrm|", "i   |i|");
    }

    @Test
    public void formatsNamesWithCaseAndPadding() {
        assertFormat(BASE, "MONTH|Month|month|MON|Mon|mon", "AUGUST   |August   |august   |AUG|Aug|aug");
        assertFormat(BASE, "DAY|Day|day|DY|Dy|dy", "THURSDAY |Thursday |thursday |THU|Thu|thu");
        assertFormat(BASE, "FMDay, FMMonth FMDD, YYYY", "Thursday, August 13, 2020");
    }

    @Test
    public void formatsTimePatterns() {
        assertFormat(BASE, "HH|HH12|HH24|MI|SS|SSSS|SSSSS", "02|02|14|05|09|50709|50709");
        assertFormat(BASE, "MS|US|FF1|FF3|FF6|FF9|FF", "123|123456|1|123|123456|123456000|123456");
        assertFormat(LocalDateTime.of(2020, 8, 13, 0, 30, 0, 5_000_000), "HH12 AM FMMS", "12 AM 005");
    }

    @Test
    public void fillModeRemovesPaddingFromTheNextPatternOnly() {
        assertFormat(LocalDateTime.of(2020, 1, 5, 7, 0), "FMMM/FMDD/YYYY FMHH24:MI", "1/5/2020 7:00");
        assertFormat(LocalDateTime.of(2020, 1, 5, 7, 0), "FMMM/DD", "1/05");
    }

    @Test
    public void appendsOrdinalSuffixes() {
        assertFormat(LocalDateTime.of(2020, 8, 1, 0, 0), "FMDDth", "1st");
        assertFormat(LocalDateTime.of(2020, 8, 22, 0, 0), "DDTH", "22ND");
        assertFormat(LocalDateTime.of(2020, 8, 23, 0, 0), "DDth", "23rd");
        assertFormat(LocalDateTime.of(2020, 8, 11, 0, 0), "DDTH", "11TH");
        assertFormat(BASE, "FMDDDth", "226th");
    }

    @Test
    public void copiesQuotedAndUnmatchedText() {
        assertFormat(BASE, "\"Quarter\" Q", "Quarter 3");
        assertFormat(BASE, "\"a\\\"b\" \\\"", "a\"b \"");
        assertFormat(BASE, "YYYY! @ %", "2020! @ %");
        assertFormat(BASE, "FXTMYYYY", "2020");
    }

    @Test
    public void formatsDateAsMidnight() {
        Datum result = DATE_FUNCTION.invoke(new Datum[] {
            Datum.date(LocalDate.of(2020, 8, 13)),
            Datum.string("YYYY-MM-DD HH24:MI:SS|FF|")
        });

        assertEquals(PType.string(), result.getType());
        assertEquals("2020-08-13 00:00:00||", result.getString());
    }

    @Test
    public void rejectsTimeZonePatterns() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> format(BASE, "YYYY TZ")
        );

        assertEquals("Time zone pattern is not supported for to_char without a time zone: TZ", error.getMessage());
    }

    private static void assertFormat(LocalDateTime value, String template, String expected) {
        assertEquals(expected, format(value, template), template);
    }

    private static String format(LocalDateTime value, String template) {
        return TIMESTAMP_FUNCTION.invoke(new Datum[] {
            Datum.timestamp(value, 6),
            Datum.string(template)
        }).getString();
    }
}
