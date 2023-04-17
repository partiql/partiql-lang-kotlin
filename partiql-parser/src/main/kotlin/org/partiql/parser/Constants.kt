package org.partiql.parser

internal enum class DateTimePart {
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE;

    companion object {

        val tokens = DateTimePart.values().map { it.toString().toLowerCase() }.toSet()

        fun safeValueOf(value: String): DateTimePart? = try {
            DateTimePart.valueOf(value.toUpperCase().trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

internal val DATE_PATTERN_REGEX = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

internal val GENERIC_TIME_REGEX = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")
