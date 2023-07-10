package org.partiql.datetime

import com.amazon.ionelement.api.ionTimestamp
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import kotlin.test.assertEquals
import com.amazon.ion.Timestamp as TimestampIon

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
        val expectedTimestamp: Timestamp,
        val expectedIonEquivalent: TimestampIon
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
        val expectedTimestamp: Timestamp,
        val expectedIonEquivalent: TimestampIon
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
        val expectedTimestamp: Timestamp
    ) : TimestampTestCase("Success Case With No Time Zone - $name")

    data class EqualsAndCompareToTest(
        val timestamp1: Timestamp,
        val timestamp2: Timestamp,
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
        val actualTimestamp = DateTimeValue.timestamp(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second, TimeZone.UtcOffset.of(tc.tzHour, tc.tzMinute)
        ).let {
            if (tc.precision != null) it.toPrecision(tc.precision)
            else it
        } as TimestampWithTimeZone

        assert(tc.expectedTimestamp == actualTimestamp) {
            """
                Expected PartiQL Timestamp : ${tc.expectedTimestamp},
                Actual PartiQL Timestamp   : $actualTimestamp
            """.trimIndent()
        }
        assert(tc.expectedIonEquivalent == actualTimestamp.ionTimestampValue) {
            """
                Expected Ion Timestamp : ${tc.expectedIonEquivalent},
                Actual Ion Timestamp   : ${actualTimestamp.ionTimestampValue}
            """.trimIndent()
        }
        assert(tc.expectedIonEquivalent.decimalMillis == actualTimestamp.epochMillis) {
            """
                Expected epoch in Millis : ${tc.expectedIonEquivalent.decimalMillis},
                Actual epoch in Millis   : ${actualTimestamp.epochMillis}
            """.trimIndent()
        }
        val timestampFromIon = DateTimeValue.timestamp(tc.expectedIonEquivalent)
        // ion Timestamp all arbitrary position
        val expectedTimestampInArbitraryPrecision =
            tc.expectedTimestamp
        assert(timestampFromIon == expectedTimestampInArbitraryPrecision) {
            """
                Expected Timestamp From Ion : $expectedTimestampInArbitraryPrecision,
                Actual Timestamp From Ion : $timestampFromIon
            """.trimIndent()
        }
    }

    private fun testSuccessCaseWithNoTimeZone(tc: SuccessCaseWithNoTimeZone) {
        val actualTimestamp = DateTimeValue.timestamp(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second
        ).let {
            if (tc.precision != null) it.toPrecision(tc.precision)
            else it
        } as TimestampWithoutTimeZone
        assertEquals(tc.expectedTimestamp, actualTimestamp)
    }

    private fun testSuccessCaseWithUnknownTimeZone(tc: SuccessCaseWithUnknownTimeZone) {
        val actualTimestamp = DateTimeValue.timestamp(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second, TimeZone.UnknownTimeZone
        ).let {
            if (tc.precision != null) it.toPrecision(tc.precision)
            else it
        } as TimestampWithTimeZone

        assert(tc.expectedTimestamp == actualTimestamp) {
            """
                Expected PartiQL Timestamp : ${tc.expectedTimestamp},
                Actual PartiQL Timestamp   : $actualTimestamp
            """.trimIndent()
        }
        assert(tc.expectedIonEquivalent == actualTimestamp.ionTimestampValue) {
            """
                Expected Ion Timestamp : ${tc.expectedIonEquivalent},
                Actual Ion Timestamp   : ${actualTimestamp.ionTimestampValue}
            """.trimIndent()
        }
        assert(tc.expectedIonEquivalent.decimalMillis == actualTimestamp.epochMillis) {
            """
                Expected epoch in Millis : ${tc.expectedIonEquivalent.decimalMillis},
                Actual epoch in Millis   : ${actualTimestamp.epochMillis}
            """.trimIndent()
        }
        val timestampFromIon = DateTimeValue.timestamp(tc.expectedIonEquivalent)
        // ion Timestamp all arbitrary position
        val expectedTimestampInArbitraryPrecision = tc.expectedTimestamp
        assert(timestampFromIon == expectedTimestampInArbitraryPrecision) {
            """
                Expected Timestamp From Ion : $expectedTimestampInArbitraryPrecision,
                Actual Timestamp From Ion : $timestampFromIon
            """.trimIndent()
        }
    }

    private fun testEqualsAndCompareToTest(tc: EqualsAndCompareToTest) {
        // Equals
        assert((tc.timestamp1 == tc.timestamp2) == tc.expectedEqualsRes) {
            """
                Timestamp 1 : ${tc.timestamp1},
                Timestamp 2 : ${tc.timestamp2},
                Expected Equals method result : ${tc.expectedEqualsRes}
                Actual Equals method result : ${tc.timestamp1 == tc.timestamp2}
            """.trimIndent()
        }
        // Compare To:
        assert((tc.timestamp1.compareTo(tc.timestamp2)) == tc.expectedCompareToRes) {
            """
                Timestamp 1 : ${tc.timestamp1},
                Timestamp 2 : ${tc.timestamp2},
                Expected Equals method result : ${when (tc.expectedCompareToRes) {0 -> "Equals" 1 -> "More" else -> "Less"}}
                Actual Equals method result : ${when (tc.timestamp1.compareTo(tc.timestamp2)) {0 -> "Equals" 1 -> "More" else -> "Less"}}
            """.trimIndent()
        }
    }

    private fun testFailedTestCases(tc: FailedTest) {
        assertThrows<DateTimeException> {
            tc.statement()
        }
    }

    companion object {
        private final val LESS = -1
        private final val EQUALS = 0
        private final val MORE = 1

        private val successCaseWithKnownTimeZone = listOf(
            SuccessCaseWithKnownTimeZone(
                "Unix Zero",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0, 0, null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix Zero with fraction decimalSecond",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00.000Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(10, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:10Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 1, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:01:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(1, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T01:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 2),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-02T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 2, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-02-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1971, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1971-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 0, 0, 5,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(100000, 5), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:01.00000Z").timestampValue
            ),
            // large precision
            SuccessCaseWithKnownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 0, 0, 20,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 20), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00.00000000000000000000Z").timestampValue
            ),
            // negative epoch
            // May be Ion has a bug?
            // Expected epoch in Millis : -23225270400000,
            // Actual epoch in Millis   : -23225875200000
            // SuccessCaseWithKnownTimeZone(
            //    1234, 1, 1, 0,0, BigDecimal.valueOf(0,3), 0, 0, 0,
            //    Timestamp.of(
            //        DateTimeValue.date(1234, 1, 1),
            //        DateTimeValue.time(0,0, BigDecimal.valueOf(0,0), TimeZone.UtcOffset.of(0))
            // ),
            // ionTimestamp("1234-01-01T00:00:00Z").timestampValue
            // ),
            SuccessCaseWithKnownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1901, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1901-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -7, 0, 3,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(-7, 0))
                ),
                ionTimestamp("1970-01-01T00:00:00.000-07:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone large hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -23, 0, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(-23, 0))
                ),
                ionTimestamp("1970-01-01T00:00:00-23:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -23, -59, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(-23, -59))
                ),
                ionTimestamp("1970-01-01T00:00:00-23:59").timestampValue
            ),
        )

        private val successCaseWithUnKnownTimeZone = listOf(
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00.000-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone with precision ",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(10, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:10-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 1, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:01:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(1, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T01:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 2),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-02T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 2, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-02-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1971, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1971-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 20), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00.00000000000000000000-00:00").timestampValue
            ),
            // negative epoch
            // May be Ion has a bug?
            // Expected epoch in Millis : -23225270400000,
            // Actual epoch in Millis   : -23225875200000
            // SuccessCaseWithUnknownTimeZone(
            //    1234, 1, 1, 0,0, BigDecimal.valueOf(0,3), 0,
            //    Timestamp.of(
            //        DateTimeValue.date(1234, 1, 1),
            //        DateTimeValue.time(0,0, BigDecimal.valueOf(0,0), TimeZone.UnknownTimeZone)
            // ),
            // ionTimestamp("1234-01-01T00:00:00-00:00").timestampValue
            // ),
            SuccessCaseWithUnknownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1901, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1901-01-01T00:00:00-00:00").timestampValue
            ),
            // padding
            SuccessCaseWithUnknownTimeZone(
                "Padding zero",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(100000, 5), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:01.00000-00:00").timestampValue
            ),
        )

        private val successCaseWithNoTimeZone = listOf(
            SuccessCaseWithNoTimeZone(
                "Unix zero",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO, null)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 3), null)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.ZERO,)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(10, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 1, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(1, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 2),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 2, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1971, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 20),)
                ),
            ),
            // negative epoch
            // May be Ion has a bug?
            // Expected epoch in Millis : -23225270400000,
            // Actual epoch in Millis   : -23225875200000
            // SuccessCaseWithNoTimeZone(
            //    1234, 1, 1, 0,0, BigDecimal.valueOf(0,3), 0,
            //    Timestamp.of(
            //        DateTimeValue.date(1234, 1, 1),
            //        DateTimeValue.time(0,0, BigDecimal.valueOf(0,0), )
            // ),
            // ),
            SuccessCaseWithNoTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1901, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(0, 0),)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                DateTimeValue.timestamp(
                    DateTimeValue.date(1970, 1, 1),
                    DateTimeValue.time(0, 0, BigDecimal.valueOf(100000, 5),)
                ),
            ),
        )

        private val equalsAndCompareToTestcase = listOf(
            // No Timezone
            // equals
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L))
                ),
                true,
                EQUALS
            ),
            // different fraction
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(90L, 1))
                ),
                false,
                EQUALS
            ),
            // larger
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 8, BigDecimal.valueOf(9L))
                ),
                false,
                MORE
            ),
            // Less
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(10, 8, BigDecimal.valueOf(9L))
                ),
                false,
                LESS
            ),

            // Known / Unknown time zone
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                EQUALS
            ),
            // fraction precision
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(90L, 1), TimeZone.UtcOffset.of(0))
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(90L, 1), TimeZone.UnknownTimeZone)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                EQUALS
            ),
            // Time Zone Hour
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(8, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 0))
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(8, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 0))
                ),
                false,
                EQUALS
            ),
            // Time zone hour minute
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(8, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 30))
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(8, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 30))
                ),
                false,
                EQUALS
            ),

            // larger
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                MORE
            ),
            // less
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone)
                ),
                DateTimeValue.timestamp(
                    DateTimeValue.date(1234, 5, 6),
                    DateTimeValue.time(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0))
                ),
                false,
                LESS
            ),
        )

        private val failedTestCases = listOf(
            // day
            FailedTest("Year more than for digits") { DateTimeValue.date(12345, 1, 1) },
            FailedTest("Month more than two digits") { DateTimeValue.date(1234, 123, 1) },
            FailedTest("Day more than two digits") { DateTimeValue.date(1234, 12, 123) },
            FailedTest("Year less than zero") { DateTimeValue.date(-1234, 12, 1) },
            FailedTest("Month less than zero") { DateTimeValue.date(1234, -12, 1) },
            FailedTest("Day less than zero") { DateTimeValue.date(1234, -12, -1) },
            FailedTest("Month > 12") { DateTimeValue.date(1234, 13, 1) },
            FailedTest("Day > 31") { DateTimeValue.date(1234, 1, 32) },
            FailedTest("not leap") { DateTimeValue.date(2023, 2, 29) },
            // time
            FailedTest("hour 3 digits") { DateTimeValue.time(123, 1, BigDecimal.ZERO) },
            FailedTest("minute 3 digits") { DateTimeValue.time(12, 111, BigDecimal.ZERO) },
            FailedTest("whole decimalSecond large than 2") { DateTimeValue.time(12, 1, BigDecimal.valueOf(1000L, 1)) },
            FailedTest("Timezone hour more than 3 digits") { DateTimeValue.time(12, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(100, 0)) },
            FailedTest("Time zone minutes more than 3 digits") { DateTimeValue.time(12, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(1, 100)) },
            FailedTest("Hour more than 24") { DateTimeValue.time(25, 1, BigDecimal.ZERO) },
            FailedTest("Minute more than 60") { DateTimeValue.time(12, 61, BigDecimal.ZERO) },
            FailedTest("Second more than 60") { DateTimeValue.time(12, 1, BigDecimal.valueOf(61L)) },

            // Timestamp comparison
            FailedTest("Compare with time zone and without time zone") {
                DateTimeValue.timestamp(1234, 5, 6, 7, 8, BigDecimal.ZERO)
                    .compareTo(DateTimeValue.timestamp(1234, 5, 6, 7, 8, BigDecimal.ZERO, TimeZone.UtcOffset.of(0)))
            }
        )

        @JvmStatic
        fun allTestCases() =
            successCaseWithKnownTimeZone + successCaseWithUnKnownTimeZone + successCaseWithNoTimeZone + equalsAndCompareToTestcase + failedTestCases
    }
}
