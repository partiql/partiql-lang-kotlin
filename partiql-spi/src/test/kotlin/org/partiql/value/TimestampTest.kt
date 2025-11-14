package org.partiql.value

import com.amazon.ionelement.api.ionTimestamp
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class TimestampTest {

    sealed class TimestampTestCase(
        open val name: String
    )

    data class SuccessCaseWithKnownTimeZone(
        override val name: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: BigDecimal,
        val tzHour: Int,
        val tzMinute: Int,
        val precision: Int?,
        val expectedTimestamp: Datum,
        val expectedIonEquivalent: com.amazon.ion.Timestamp
    ) : TimestampTestCase("Success Case With Known Time Zone - $name")

    data class SuccessCaseWithUnknownTimeZone(
        override val name: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: BigDecimal,
        val precision: Int?,
        val expectedTimestamp: Datum,
        val expectedIonEquivalent: com.amazon.ion.Timestamp
    ) : TimestampTestCase("Success Case With Unknown Time Zone - $name")

    data class SuccessCaseWithNoTimeZone(
        override val name: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: BigDecimal,
        val precision: Int?,
        val expectedTimestamp: Datum
    ) : TimestampTestCase("Success Case With No Time Zone - $name")

    data class EqualsAndCompareToTest(
        val timestamp1: Datum,
        val timestamp2: Datum,
        val expectedEqualsRes: Boolean,
        val expectedCompareToRes: Int
    ) : TimestampTestCase("$timestamp1.equals($timestamp2) : $expectedEqualsRes; $timestamp1.compareTo($timestamp2) : $expectedEqualsRes;")

    data class FailedTest(
        override val name: String,
        val statement: () -> Any
    ) : TimestampTestCase(name)

    @ParameterizedTest
    @MethodSource("allTestCases")
    fun runTests(tc: TimestampTestCase) {
        when (tc) {
            is SuccessCaseWithKnownTimeZone -> testSuccessCaseWithKnownTimeZone(tc)
            is SuccessCaseWithNoTimeZone -> testSuccessCaseWithNoTimeZone(tc)
            is SuccessCaseWithUnknownTimeZone -> testSuccessCaseWithUnknownTimeZone(tc)
            is EqualsAndCompareToTest -> testEqualsAndCompareToTest(tc)
            is FailedTest -> testFailedTestCases(tc)
        }
    }

    private fun testSuccessCaseWithKnownTimeZone(tc: SuccessCaseWithKnownTimeZone) {
        val offsetDateTime = OffsetDateTime.of(
            tc.year, tc.month, tc.day, tc.hour, tc.minute,
            tc.second.toInt(), (tc.second.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).toInt()),
            ZoneOffset.ofHoursMinutes(tc.tzHour, tc.tzMinute)
        )
        val actualTimestamp = Datum.timestampz(offsetDateTime, tc.precision ?: 6)
        val comparator = Datum.comparator()
        assertEquals(0, comparator.compare(tc.expectedTimestamp, actualTimestamp))
    }

    private fun testSuccessCaseWithNoTimeZone(tc: SuccessCaseWithNoTimeZone) {
        val localDateTime = LocalDateTime.of(
            tc.year, tc.month, tc.day, tc.hour, tc.minute,
            tc.second.toInt(), (tc.second.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).toInt())
        )
        val actualTimestamp = Datum.timestamp(localDateTime, tc.precision ?: 6)
        val comparator = Datum.comparator()
        assertEquals(0, comparator.compare(tc.expectedTimestamp, actualTimestamp))
    }

    private fun testSuccessCaseWithUnknownTimeZone(tc: SuccessCaseWithUnknownTimeZone) {
        // For unknown timezone, use offset -00:00 as per Ion specification
        val offsetDateTime = OffsetDateTime.of(
            tc.year, tc.month, tc.day, tc.hour, tc.minute,
            tc.second.toInt(), (tc.second.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).toInt()),
            ZoneOffset.ofHours(0)
        )
        val actualTimestamp = Datum.timestampz(offsetDateTime, tc.precision ?: 6)
        val comparator = Datum.comparator()
        assertEquals(0, comparator.compare(tc.expectedTimestamp, actualTimestamp))
    }

    private fun testEqualsAndCompareToTest(tc: EqualsAndCompareToTest) {
        val comparator = Datum.comparator()
        val actualCompareResult = comparator.compare(tc.timestamp1, tc.timestamp2)
        assertEquals(tc.expectedCompareToRes, actualCompareResult)
    }

    private fun testFailedTestCases(tc: FailedTest) {
        assertThrows<Exception> {
            tc.statement()
        }
    }

    companion object {
        private const val LESS = -1
        private const val EQUALS = 0
        private const val MORE = 1

        private val successCaseWithKnownTimeZone = listOf(
            SuccessCaseWithKnownTimeZone(
                "Unix Zero",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0, 0, null,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 6
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix Zero with fraction decimalSecond",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, null,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 6
                ),
                ionTimestamp("1970-01-01T00:00:00.000Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves fractional seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 9, 999990000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T00:00:09.99999Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves 59.9 seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 59, 900000000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T00:00:59.9Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves 59:59.9",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 59, 59, 900000000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T00:59:59.9Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves 23:59:59.9",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 23, 59, 59, 900000000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-01T23:59:59.9Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves Jan 31 23:59:59.9",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 31, 23, 59, 59, 900000000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-01-31T23:59:59.9Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "No rounding - preserves Dec 31 23:59:59.9",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 12, 31, 23, 59, 59, 900000000, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1970-12-31T23:59:59.9Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 0, 0, 5,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC), 5
                ),
                ionTimestamp("1970-01-01T00:00:01.00000Z").timestampValue
            ),
            // large precision
            SuccessCaseWithKnownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 0, 0, 20,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 20
                ),
                ionTimestamp("1970-01-01T00:00:00.00000000000000000000Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1901, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 0
                ),
                ionTimestamp("1901-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -7, 0, 3,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-7)), 3
                ),
                ionTimestamp("1970-01-01T00:00:00.000-07:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone large hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -18, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), 0
                ),
                ionTimestamp("1970-01-01T00:00:00-18:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -18, 0, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), 0
                ),
                ionTimestamp("1970-01-01T00:00:00-18:00").timestampValue
            ),
        )

        private val successCaseWithUnKnownTimeZone = listOf(
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 6
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 6
                ),
                ionTimestamp("1970-01-01T00:00:00.000-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone with precision ",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves 9.99999 seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 9, 999990000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T00:00:09.99999-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves 59.9 seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 59, 900000000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T00:00:59.9-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves 59:59.9",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 59, 59, 900000000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T00:59:59.9-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves 23:59:59.9",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 23, 59, 59, 900000000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-01T23:59:59.9-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves Jan 31 23:59:59.9",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 31, 23, 59, 59, 900000000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-01-31T23:59:59.9-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "No rounding - preserves Dec 31 23:59:59.9",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 12, 31, 23, 59, 59, 900000000, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1970-12-31T23:59:59.9-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 20
                ),
                ionTimestamp("1970-01-01T00:00:00.00000000000000000000-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Datum.timestampz(
                    OffsetDateTime.of(1901, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 0
                ),
                ionTimestamp("1901-01-01T00:00:00-00:00").timestampValue
            ),
            // padding
            SuccessCaseWithUnknownTimeZone(
                "Padding zero",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                Datum.timestampz(
                    OffsetDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneOffset.ofHours(0)), 5
                ),
                ionTimestamp("1970-01-01T00:00:01.00000-00:00").timestampValue
            ),
        )

        private val successCaseWithNoTimeZone = listOf(
            SuccessCaseWithNoTimeZone(
                "Unix zero",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 6
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 6
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves 9.99999 seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 9, 999990000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves 59.9 seconds",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 59, 900000000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves 59:59.9",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 59, 59, 900000000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves 23:59:59.9",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 23, 59, 59, 900000000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves Jan 31 23:59:59.9",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 31, 23, 59, 59, 900000000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "No rounding - preserves Dec 31 23:59:59.9",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Datum.timestamp(
                    LocalDateTime.of(1970, 12, 31, 23, 59, 59, 900000000), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 20
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Negative epoch",
                1234, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Datum.timestamp(
                    LocalDateTime.of(1234, 1, 1, 0, 0, 0, 0), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Datum.timestamp(
                    LocalDateTime.of(1901, 1, 1, 0, 0, 0, 0), 0
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                Datum.timestamp(
                    LocalDateTime.of(1970, 1, 1, 0, 0, 1, 0), 5
                ),
            ),
        )

        private val equalsAndCompareToTestcase = listOf(
            // No Timezone
            // equals
            EqualsAndCompareToTest(
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9), 6
                ),
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9), 6
                ),
                true,
                EQUALS
            ),
            // different fraction
            EqualsAndCompareToTest(
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9), 6
                ),
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9, 100000000), 6
                ),
                false,
                LESS
            ),
            // larger
            EqualsAndCompareToTest(
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9), 6
                ),
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 5, 8, 9), 6
                ),
                false,
                MORE
            ),
            // Less
            EqualsAndCompareToTest(
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 7, 8, 9), 6
                ),
                Datum.timestamp(
                    LocalDateTime.of(1234, 5, 6, 10, 8, 9), 6
                ),
                false,
                LESS
            ),

            // Known / Unknown time zone
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                false,
                EQUALS
            ),
            // fraction precision
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 100000000, ZoneOffset.UTC), 6
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 100000000, ZoneOffset.ofHours(0)), 6
                ),
                false,
                LESS
            ),
            // Time Zone Hour
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 8, 8, 9, 0, ZoneOffset.ofHours(1)), 6
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 8, 8, 9, 0, ZoneOffset.ofHours(1)), 6
                ),
                false,
                EQUALS
            ),
            // Time zone hour minute
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 8, 38, 9, 0, ZoneOffset.ofHoursMinutes(1, 30)), 6
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 8, 38, 9, 0, ZoneOffset.ofHoursMinutes(1, 30)), 6
                ),
                false,
                EQUALS
            ),

            // larger
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.UTC), 6
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                false,
                MORE
            ),
            // less
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.UTC), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 5, 38, 9, 0, ZoneOffset.ofHours(0)), 6
                ),
                Datum.timestampz(
                    OffsetDateTime.of(1234, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC), 6
                ),
                false,
                LESS
            ),
        )

        private val failedTestCases = listOf(
            // Invalid date/time values - these will be caught by LocalDateTime/OffsetDateTime constructors
            FailedTest("Month > 12") { Datum.date(LocalDate.of(1234, 13, 1)) },
            FailedTest("Day > 31") { Datum.date(LocalDate.of(1234, 1, 32)) },
            FailedTest("not leap") { Datum.date(LocalDate.of(2023, 2, 29)) },
            FailedTest("Hour more than 24") { Datum.timestamp(LocalDateTime.of(1234, 1, 1, 25, 1, 0), 6) },
            FailedTest("Minute more than 60") { Datum.timestamp(LocalDateTime.of(1234, 1, 1, 12, 61, 0), 6) },
            FailedTest("Second more than 60") { Datum.timestamp(LocalDateTime.of(1234, 1, 1, 12, 1, 61), 6) }
        )

        @JvmStatic
        fun allTestCases() = successCaseWithKnownTimeZone + successCaseWithUnKnownTimeZone + successCaseWithNoTimeZone + equalsAndCompareToTestcase + failedTestCases
    }
}
