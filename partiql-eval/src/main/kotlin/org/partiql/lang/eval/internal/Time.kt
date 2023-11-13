package org.partiql.lang.eval.internal

// Constants related to the TIME

internal const val HOURS_PER_DAY = 24
internal const val MINUTES_PER_HOUR = 60
internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val NANOS_PER_SECOND = 1000000000
internal const val MAX_PRECISION_FOR_TIME = 9

internal enum class DateTimePart {
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE;

    companion object {
        fun safeValueOf(value: String): DateTimePart? = try {
            valueOf(value.uppercase().trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
