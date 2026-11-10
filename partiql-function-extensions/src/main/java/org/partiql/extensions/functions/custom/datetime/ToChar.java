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
 * <p>Like PostgreSQL, patterns are matched longest first, in upper case or lower case ({@code YYYY}, {@code yyyy}),
 * and name patterns also capitalized ({@code Month}, {@code Mon}, {@code Day}, {@code Dy}). Any other mix of case is
 * text, e.g. {@code MONth} is {@code MON} followed by {@code th}. The letter case of name patterns ({@code MONTH},
 * {@code Month}, {@code month}, and likewise {@code MON}, {@code DAY}, {@code DY}, {@code RM}, {@code AM},
 * {@code A.M.}, {@code AD}, {@code B.C.}, ...) selects the letter case of the output.
 *
 * <ul>
 *     <li>Year: {@code Y,YYY YYYY YYY YY Y RRRR RR CC IYYY IYY IY I AD BC A.D. B.C.}</li>
 *     <li>Month and quarter: {@code MM MONTH MON RM Q}</li>
 *     <li>Week: {@code WW W IW}</li>
 *     <li>Day: {@code DDD DD D IDDD ID DAY DY J}</li>
 *     <li>Time: {@code HH HH12 HH24 MI SS SSSS SSSSS MS US FF FF1-FF9 AM PM A.M. P.M.}</li>
 *     <li>Prefixes, at most one per pattern: {@code FM} (no padding for the next pattern) and {@code TM} (names
 *     without blank padding; names are always English)</li>
 *     <li>{@code FX}: accepted and prints nothing (it only affects parsing)</li>
 *     <li>Suffix: {@code TH} / {@code th} (ordinal suffix for the preceding numeric pattern; ignored after a text
 *     pattern)</li>
 * </ul>
 *
 * <p>Double-quoted text is copied literally ({@code \"} inside quotes is a quote); {@code \"} outside quotes
 * produces a quote; any other character is copied. Names are English. Without {@code FM}, {@code MONTH} and
 * {@code DAY} are blank-padded to 9 characters, {@code RM} to 4, and numbers are zero-padded to their width.
 * Where PostgreSQL and Oracle differ, PostgreSQL behavior is used ({@code D} is 1 for Sunday; {@code SSSS} is not
 * padded). Years before year 1 are shown as positive years with {@code BC}, and {@code CC} is negative for them
 * ({@code -01} for 44 BC). {@code FF} without digits uses the precision of the timestamp. Time zone patterns ({@code TZ},
 * {@code TZH}, {@code TZM}, {@code OF}) are rejected because the inputs have no time zone.
 * A date argument is treated as midnight of that day. Like PostgreSQL and Oracle, an empty template returns null.
 */
public final class ToChar {
    private static final List<FnOverload> OVERLOADS = Collections.unmodifiableList(Arrays.asList(
        new FnOverload.Builder("to_char")
            .addParameters(PType.timestamp(), PType.string())
            .returns(PType.string())
            .isNullCall(true)
            .body(args -> result(format(
                args[0].getLocalDateTime(),
                args[0].getType().getPrecision(),
                args[1].getString()
            )))
            .build(),
        new FnOverload.Builder("to_char")
            .addParameters(PType.date(), PType.string())
            .returns(PType.string())
            .isNullCall(true)
            .body(args -> result(format(
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
            "FF1", "FF2", "FF3", "FF4", "FF5", "FF6", "FF7", "FF8", "FF9", "FF", "FX",
            "HH24", "HH12", "HH", "IDDD", "ID", "IW", "IYYY", "IYY", "IY", "I", "J",
            "MI", "MM", "MONTH", "MON", "MS", "OF", "P.M.", "PM", "Q", "RM", "RRRR", "RR",
            "SSSSS", "SSSS", "SS", "TZH", "TZM", "TZ", "US", "WW", "W",
            "Y,YYY", "YYYY", "YYY", "YY", "Y"
        );
        String[] sorted = patterns.toArray(new String[0]);
        Arrays.sort(sorted, Comparator.comparingInt(String::length).reversed());
        PATTERNS = Collections.unmodifiableList(Arrays.asList(sorted));
    }

    private static final List<String> PREFIXES = Collections.unmodifiableList(Arrays.asList("FM", "TM"));

    private static final List<String> NAME_PATTERNS = Collections.unmodifiableList(Arrays.asList(
        "MONTH", "MON", "DAY", "DY"
    ));

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

    private static Datum result(String formatted) {
        return formatted == null ? Datum.nullValue(PType.string()) : Datum.string(formatted);
    }

    /**
     * Returns the formatted [value], or null if [template] is empty.
     */
    private static String format(LocalDateTime value, int precision, String template) {
        if (template.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = template.length();
        while (i < n) {
            // Like PostgreSQL, at most one prefix. It is consumed even when no pattern follows; the text after it is
            // then copied (or quoted) as usual, and a second prefix is copied as text.
            String prefix = matchPrefix(template, i);
            int start = prefix == null ? i : i + 2;
            String pattern = match(template, start);
            if (pattern == null) {
                i = copyText(template, start, out);
                continue;
            }
            boolean fillMode = "FM".equals(prefix);
            boolean translationMode = "TM".equals(prefix);
            String text = template.substring(start, start + pattern.length());
            i = start + pattern.length();
            if (pattern.equals("FF") && precision > 0) {
                // Oracle: FF without digits uses the precision of the timestamp.
                pattern = "FF" + Math.min(precision, 9);
            }

            // Like PostgreSQL, a TH / th suffix is consumed after any pattern, but only numbers get an ordinal.
            boolean ordinalSuffix = template.startsWith("TH", i) || template.startsWith("th", i);
            boolean upperOrdinal = ordinalSuffix && template.charAt(i) == 'T';
            if (ordinalSuffix) {
                i += 2;
            }
            Long number = number(pattern, value, precision);
            if (number != null) {
                String digits = pad(pattern, number, fillMode);
                out.append(digits);
                if (ordinalSuffix) {
                    String suffix = ordinal(digits);
                    out.append(upperOrdinal ? suffix.toUpperCase(Locale.ROOT) : suffix);
                }
            } else {
                // Like PostgreSQL, TM (translated names) also drops the blank padding of names.
                out.append(text(pattern, text, value, fillMode || translationMode));
            }
        }
        return out.toString();
    }

    /**
     * Copies the text at [start] that is not a pattern: a double-quoted string, an escaped quote, or one character.
     * Returns the offset after it.
     */
    private static int copyText(String template, int start, StringBuilder out) {
        if (start >= template.length()) {
            return start;
        }
        char c = template.charAt(start);
        if (c == '"') {
            return copyQuoted(template, start + 1, out);
        }
        if (c == '\\' && start + 1 < template.length() && template.charAt(start + 1) == '"') {
            out.append('"');
            return start + 2;
        }
        out.append(c);
        return start + 1;
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

    /**
     * Returns the upper-case prefix ({@code FM} or {@code TM}) at [offset], or null.
     */
    private static String matchPrefix(String template, int offset) {
        for (String prefix : PREFIXES) {
            if (matchesCase(template, offset, prefix, false)) {
                return prefix;
            }
        }
        return null;
    }

    private static String match(String template, int offset) {
        for (String pattern : PATTERNS) {
            if (matchesCase(template, offset, pattern, NAME_PATTERNS.contains(pattern))) {
                return pattern;
            }
        }
        return null;
    }

    /**
     * Like PostgreSQL, a pattern matches only in upper case or lower case, and a name pattern also when capitalized,
     * e.g. {@code MONTH}, {@code month}, {@code Month}, but not {@code MONth}.
     */
    private static boolean matchesCase(String template, int offset, String pattern, boolean allowCapitalized) {
        if (!template.regionMatches(true, offset, pattern, 0, pattern.length())) {
            return false;
        }
        String text = template.substring(offset, offset + pattern.length());
        if (text.equals(pattern) || text.equals(pattern.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return allowCapitalized
            && text.equals(pattern.charAt(0) + pattern.substring(1).toLowerCase(Locale.ROOT));
    }

    private static int displayYear(LocalDateTime value) {
        int year = value.getYear();
        return year > 0 ? year : 1 - year;
    }

    /**
     * Returns the ISO 8601 week-based year as a positive display year, like [displayYear].
     */
    private static int isoYear(LocalDateTime value) {
        int year = value.get(IsoFields.WEEK_BASED_YEAR);
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
                // Like PostgreSQL, centuries before year 1 are negative: 44 BC is century -1.
                return value.getYear() > 0 ? (long) ((value.getYear() - 1) / 100 + 1) : (long) (value.getYear() / 100 - 1);
            case "IYYY":
                return (long) isoYear(value);
            case "IYY":
                return (long) (isoYear(value) % 1000);
            case "IY":
                return (long) (isoYear(value) % 100);
            case "I":
                return (long) (isoYear(value) % 10);
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
        int width = fillMode && !fractional ? 0 : width(pattern);
        if (number < 0) {
            // Like PostgreSQL, a negative century is "-01": the sign comes before the zero padding.
            return "-" + zeroPad(Long.toString(-number), width);
        }
        return zeroPad(Long.toString(number), width);
    }

    private static String zeroPad(String digits, int width) {
        StringBuilder padded = new StringBuilder();
        for (int i = digits.length(); i < width; i++) {
            padded.append('0');
        }
        return padded.append(digits).toString();
    }

    private static String ordinal(String digits) {
        // Only the last two digits matter; a sign or separator (-01, 2,024) is not part of them.
        String unsigned = digits.replaceAll("[^0-9]", "");
        int value = Integer.parseInt(unsigned.length() > 2 ? unsigned.substring(unsigned.length() - 2) : unsigned);
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
            case "FX":
                // Like PostgreSQL, FX (fixed format) is a pattern that only affects parsing, so it prints nothing.
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
