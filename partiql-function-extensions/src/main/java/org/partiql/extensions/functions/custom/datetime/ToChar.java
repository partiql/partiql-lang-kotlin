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

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * {@code to_char} overloads that format a timestamp or date with a PostgreSQL / Oracle / Redshift style template.
 *
 * <p>Template patterns are matched case-insensitively, longest first. The letter case of name patterns
 * ({@code MONTH}, {@code Month}, {@code month}, and likewise {@code MON}, {@code DAY}, {@code DY}, {@code RM},
 * {@code AM}, {@code A.M.}, {@code AD}, {@code B.C.}, ...) selects the letter case of the output.
 *
 * <ul>
 *     <li>Year: {@code Y,YYY YYYY YYY YY Y RRRR RR CC IYYY IYY IY I AD BC A.D. B.C.}</li>
 *     <li>Month and quarter: {@code MM MONTH MON RM Q}</li>
 *     <li>Week: {@code WW W IW}</li>
 *     <li>Day: {@code DDD DD D IDDD ID DAY DY J}</li>
 *     <li>Time: {@code HH HH12 HH24 MI SS SSSS SSSSS MS US FF FF1-FF9 AM PM A.M. P.M.}</li>
 *     <li>Prefixes: {@code FM} (no padding for the next pattern), {@code FX} and {@code TM} (accepted, no effect)</li>
 *     <li>Suffix: {@code TH} / {@code th} (ordinal suffix for the preceding numeric pattern)</li>
 * </ul>
 *
 * <p>Double-quoted text is copied literally ({@code \"} inside quotes is a quote); {@code \"} outside quotes
 * produces a quote; any other character is copied. Names are English. Without {@code FM}, {@code MONTH} and
 * {@code DAY} are blank-padded to 9 characters, {@code RM} to 4, and numbers are zero-padded to their width.
 * Where PostgreSQL and Oracle differ, PostgreSQL behavior is used ({@code D} is 1 for Sunday; {@code SSSS} is not
 * padded). {@code FF} without digits uses the precision of the timestamp. Time zone patterns ({@code TZ},
 * {@code TZH}, {@code TZM}, {@code OF}) are rejected because the inputs have no time zone.
 * A date argument is treated as midnight of that day.
 */
public final class ToChar {
    private static final List<FnOverload> OVERLOADS = Collections.unmodifiableList(Arrays.asList(
        new FnOverload.Builder("to_char")
            .addParameters(PType.timestamp(), PType.string())
            .returns(PType.string())
            .isNullCall(true)
            .body(args -> Datum.string(format(
                args[0].getLocalDateTime(),
                args[0].getType().getPrecision(),
                args[1].getString()
            )))
            .build(),
        new FnOverload.Builder("to_char")
            .addParameters(PType.date(), PType.string())
            .returns(PType.string())
            .isNullCall(true)
            .body(args -> Datum.string(format(
                args[0].getLocalDate().atStartOfDay(),
                0,
                args[1].getString()
            )))
            .build()
    ));

    private static final String[] ROMAN_MONTHS = {
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"
    };

    private static final List<String> PATTERNS;

    static {
        List<String> patterns = Arrays.asList(
            "A.D.", "A.M.", "AD", "AM", "B.C.", "BC", "CC", "DAY", "DDD", "DD", "DY", "D",
            "FF1", "FF2", "FF3", "FF4", "FF5", "FF6", "FF7", "FF8", "FF9", "FF",
            "HH24", "HH12", "HH", "IDDD", "ID", "IW", "IYYY", "IYY", "IY", "I", "J",
            "MI", "MM", "MONTH", "MON", "MS", "OF", "P.M.", "PM", "Q", "RM", "RRRR", "RR",
            "SSSSS", "SSSS", "SS", "TZH", "TZM", "TZ", "US", "WW", "W",
            "Y,YYY", "YYYY", "YYY", "YY", "Y"
        );
        String[] sorted = patterns.toArray(new String[0]);
        Arrays.sort(sorted, Comparator.comparingInt(String::length).reversed());
        PATTERNS = Collections.unmodifiableList(Arrays.asList(sorted));
    }

    private ToChar() {
    }

    /**
     * Returns the immutable {@code to_char} overloads.
     *
     * @return {@code to_char} overloads
     */
    public static List<FnOverload> overloads() {
        return OVERLOADS;
    }

    private static String format(LocalDateTime value, int precision, String template) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = template.length();
        while (i < n) {
            char c = template.charAt(i);
            if (c == '"') {
                i = copyQuoted(template, i + 1, out);
                continue;
            }
            if (c == '\\' && i + 1 < n && template.charAt(i + 1) == '"') {
                out.append('"');
                i += 2;
                continue;
            }

            boolean fillMode = false;
            while (true) {
                if (startsWith(template, i, "FM")) {
                    fillMode = true;
                    i += 2;
                } else if (startsWith(template, i, "FX") || startsWith(template, i, "TM")) {
                    i += 2;
                } else {
                    break;
                }
            }

            String pattern = match(template, i);
            if (pattern == null) {
                if (i < n) {
                    out.append(template.charAt(i));
                    i++;
                }
                continue;
            }
            String text = template.substring(i, i + pattern.length());
            i += pattern.length();
            if (pattern.equals("FF") && precision > 0) {
                // Oracle: FF without digits uses the precision of the timestamp.
                pattern = "FF" + Math.min(precision, 9);
            }

            Long number = number(pattern, value, precision);
            if (number != null) {
                String digits = pad(pattern, number, fillMode);
                out.append(digits);
                if (i + 1 < n && (template.startsWith("TH", i) || template.startsWith("th", i))) {
                    String suffix = ordinal(digits);
                    out.append(template.charAt(i) == 'T' ? suffix.toUpperCase(Locale.ROOT) : suffix);
                    i += 2;
                }
            } else {
                out.append(text(pattern, text, value, fillMode));
            }
        }
        return out.toString();
    }

    private static int copyQuoted(String template, int start, StringBuilder out) {
        int i = start;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '"') {
                return i + 1;
            }
            if (c == '\\' && i + 1 < template.length()) {
                out.append(template.charAt(i + 1));
                i += 2;
                continue;
            }
            out.append(c);
            i++;
        }
        return i;
    }

    private static boolean startsWith(String template, int offset, String prefix) {
        return template.regionMatches(true, offset, prefix, 0, prefix.length());
    }

    private static String match(String template, int offset) {
        for (String pattern : PATTERNS) {
            if (startsWith(template, offset, pattern)) {
                return pattern;
            }
        }
        return null;
    }

    private static int displayYear(LocalDateTime value) {
        int year = value.getYear();
        return year > 0 ? year : 1 - year;
    }

    /**
     * Returns the numeric value of [pattern], or null if the pattern produces text.
     */
    private static Long number(String pattern, LocalDateTime value, int precision) {
        int year = displayYear(value);
        switch (pattern) {
            case "Y,YYY":
            case "YYYY":
            case "RRRR":
                return (long) year;
            case "YYY":
                return (long) (year % 1000);
            case "YY":
            case "RR":
                return (long) (year % 100);
            case "Y":
                return (long) (year % 10);
            case "CC":
                return (long) ((year + 99) / 100);
            case "IYYY":
                return (long) value.get(IsoFields.WEEK_BASED_YEAR);
            case "IYY":
                return (long) (value.get(IsoFields.WEEK_BASED_YEAR) % 1000);
            case "IY":
                return (long) (value.get(IsoFields.WEEK_BASED_YEAR) % 100);
            case "I":
                return (long) (value.get(IsoFields.WEEK_BASED_YEAR) % 10);
            case "MM":
                return (long) value.getMonthValue();
            case "Q":
                return (long) ((value.getMonthValue() - 1) / 3 + 1);
            case "WW":
                return (long) ((value.getDayOfYear() - 1) / 7 + 1);
            case "W":
                return (long) ((value.getDayOfMonth() - 1) / 7 + 1);
            case "IW":
                return (long) value.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            case "DDD":
                return (long) value.getDayOfYear();
            case "DD":
                return (long) value.getDayOfMonth();
            case "D":
                return (long) (value.getDayOfWeek().getValue() % 7 + 1);
            case "IDDD":
                return (long) ((value.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) - 1) * 7
                    + value.getDayOfWeek().getValue());
            case "ID":
                return (long) value.getDayOfWeek().getValue();
            case "J":
                return value.toLocalDate().getLong(JulianFields.JULIAN_DAY);
            case "HH":
            case "HH12":
                int hour = value.getHour() % 12;
                return (long) (hour == 0 ? 12 : hour);
            case "HH24":
                return (long) value.getHour();
            case "MI":
                return (long) value.getMinute();
            case "SS":
                return (long) value.getSecond();
            case "SSSS":
            case "SSSSS":
                return (long) value.toLocalTime().toSecondOfDay();
            case "MS":
                return (long) (value.getNano() / 1_000_000);
            case "US":
                return (long) (value.getNano() / 1_000);
            case "FF1":
            case "FF2":
            case "FF3":
            case "FF4":
            case "FF5":
            case "FF6":
            case "FF7":
            case "FF8":
            case "FF9":
                return fraction(value, pattern.charAt(2) - '0');
            default:
                return null;
        }
    }

    private static long fraction(LocalDateTime value, int digits) {
        long result = value.getNano();
        for (int i = digits; i < 9; i++) {
            result /= 10;
        }
        return result;
    }

    private static int width(String pattern) {
        switch (pattern) {
            case "Y,YYY":
            case "YYYY":
            case "RRRR":
            case "IYYY":
                return 4;
            case "YYY":
            case "IYY":
            case "DDD":
            case "IDDD":
            case "MS":
                return 3;
            case "YY":
            case "RR":
            case "IY":
            case "CC":
            case "MM":
            case "WW":
            case "IW":
            case "DD":
            case "HH":
            case "HH12":
            case "HH24":
            case "MI":
            case "SS":
                return 2;
            case "US":
                return 6;
            default:
                if (pattern.startsWith("FF")) {
                    return pattern.charAt(2) - '0';
                }
                return 1;
        }
    }

    private static String pad(String pattern, long number, boolean fillMode) {
        if (pattern.equals("Y,YYY")) {
            // Like PostgreSQL, always "<thousands>,<three digits>", regardless of fill mode.
            return (number / 1000) + "," + zeroPad(Long.toString(number % 1000), 3);
        }
        // Fractional seconds are digits after a decimal point, so fill mode does not remove their leading zeros.
        boolean fractional = pattern.equals("MS") || pattern.equals("US") || pattern.startsWith("FF");
        return zeroPad(Long.toString(number), fillMode && !fractional ? 0 : width(pattern));
    }

    private static String zeroPad(String digits, int width) {
        StringBuilder padded = new StringBuilder();
        for (int i = digits.length(); i < width; i++) {
            padded.append('0');
        }
        return padded.append(digits).toString();
    }

    private static String ordinal(String digits) {
        int value = Integer.parseInt(digits.length() > 2 ? digits.substring(digits.length() - 2) : digits);
        if (value % 100 >= 11 && value % 100 <= 13) {
            return "th";
        }
        switch (value % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private static String text(String pattern, String text, LocalDateTime value, boolean fillMode) {
        switch (pattern) {
            case "MONTH":
                return padRight(withCase(value.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH), text), 9, fillMode);
            case "MON":
                return withCase(value.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), text);
            case "DAY":
                return padRight(withCase(value.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), text), 9, fillMode);
            case "DY":
                return withCase(value.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), text);
            case "RM":
                String roman = ROMAN_MONTHS[value.getMonthValue() - 1];
                return padRight(Character.isLowerCase(text.charAt(0)) ? roman.toLowerCase(Locale.ROOT) : roman, 4, fillMode);
            case "AM":
            case "PM":
                return marker(value.getHour() < 12 ? "AM" : "PM", text);
            case "A.M.":
            case "P.M.":
                return marker(value.getHour() < 12 ? "A.M." : "P.M.", text);
            case "AD":
            case "BC":
                return marker(value.getYear() > 0 ? "AD" : "BC", text);
            case "A.D.":
            case "B.C.":
                return marker(value.getYear() > 0 ? "A.D." : "B.C.", text);
            case "TZ":
            case "TZH":
            case "TZM":
            case "OF":
                throw new IllegalArgumentException(
                    "Time zone pattern is not supported for to_char without a time zone: " + text
                );
            case "FF":
                // A date or TIMESTAMP(0) has no fractional seconds.
                return "";
            default:
                throw new IllegalStateException("Unhandled to_char pattern: " + pattern);
        }
    }

    /**
     * Applies the letter case of the template [text] to [name]: all upper, capitalized, or all lower.
     */
    private static String withCase(String name, String text) {
        if (Character.isUpperCase(text.charAt(0))) {
            return text.length() > 1 && Character.isUpperCase(text.charAt(1))
                ? name.toUpperCase(Locale.ROOT)
                : name;
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static String marker(String marker, String text) {
        return Character.isLowerCase(text.charAt(0)) ? marker.toLowerCase(Locale.ROOT) : marker;
    }

    private static String padRight(String value, int width, boolean fillMode) {
        if (fillMode) {
            return value;
        }
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < width) {
            padded.append(' ');
        }
        return padded.toString();
    }
}
