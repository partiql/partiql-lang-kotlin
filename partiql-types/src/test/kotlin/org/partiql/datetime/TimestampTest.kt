package org.partiql.datetime

import com.amazon.ionelement.api.ionTimestamp
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
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
        val actualTimestamp = Timestamp.of(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second, TimeZone.UtcOffset.of(tc.tzHour, tc.tzMinute), tc.precision
        )

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
        val timestampFromIon = Timestamp.of(tc.expectedIonEquivalent)
        // ion Timestamp all arbitrary position
        val expectedTimestampInArbitraryPrecision =
            tc.expectedTimestamp.copy(precision = null)
        assert(timestampFromIon == expectedTimestampInArbitraryPrecision) {
            """
                Expected Timestamp From Ion : $expectedTimestampInArbitraryPrecision,
                Actual Timestamp From Ion : $timestampFromIon
            """.trimIndent()
        }
    }

    private fun testSuccessCaseWithNoTimeZone(tc: SuccessCaseWithNoTimeZone) {
        val actualTimestamp = Timestamp.of(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second, null, tc.precision
        )
        assertEquals(tc.expectedTimestamp, actualTimestamp)
    }

    private fun testSuccessCaseWithUnknownTimeZone(tc: SuccessCaseWithUnknownTimeZone) {
        val actualTimestamp = Timestamp.of(
            tc.year, tc.month, tc.day,
            tc.hour, tc.minute, tc.second, TimeZone.UnknownTimeZone, tc.precision
        )

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
        val timestampFromIon = Timestamp.of(tc.expectedIonEquivalent)
        // ion Timestamp all arbitrary position
        val expectedTimestampInArbitraryPrecision = tc.expectedTimestamp.copy(precision = null)
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
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix Zero with fraction second",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, null,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(0))
                ),
                ionTimestamp("1970-01-01T00:00:00.000Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(10, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-01T00:00:10Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 1, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-01T00:01:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(1, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-01T01:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 2),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-01-02T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Timestamp.of(
                    Date.of(1970, 2, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1970-02-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0, 0, 0,
                Timestamp.of(
                    Date.of(1971, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1971-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "Rounding - padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 0, 0, 5,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(100000, 5), TimeZone.UtcOffset.of(0), 5)
                ),
                ionTimestamp("1970-01-01T00:00:01.00000Z").timestampValue
            ),
            // large precision
            SuccessCaseWithKnownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 0, 0, 20,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 20), TimeZone.UtcOffset.of(0), 20)
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
            //        Date.of(1234, 1, 1),
            //        Time.of(0,0, BigDecimal.valueOf(0,0), TimeZone.UtcOffset.of(0), 0)
            // ),
            // ionTimestamp("1234-01-01T00:00:00Z").timestampValue
            // ),
            SuccessCaseWithKnownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 0,
                Timestamp.of(
                    Date.of(1901, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UtcOffset.of(0), 0)
                ),
                ionTimestamp("1901-01-01T00:00:00Z").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -7, 0, 3,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(-7, 0), 3)
                ),
                ionTimestamp("1970-01-01T00:00:00.000-07:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone large hour",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -23, 0, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(-23, 0), 0)
                ),
                ionTimestamp("1970-01-01T00:00:00-23:00").timestampValue
            ),
            SuccessCaseWithKnownTimeZone(
                "With time zone minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), -23, -59, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UtcOffset.of(-23, -59), 0)
                ),
                ionTimestamp("1970-01-01T00:00:00-23:59").timestampValue
            ),
        )

        private val successCaseWithUnKnownTimeZone = listOf(
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), TimeZone.UnknownTimeZone)
                ),
                ionTimestamp("1970-01-01T00:00:00.000-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Unix zero with unknown time zone with precision ",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(10, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-01T00:00:10-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 1, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-01T00:01:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(1, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-01T01:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 2),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-01-02T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 2, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1970-02-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Rounding - rounding carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1971, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1971-01-01T00:00:00-00:00").timestampValue
            ),
            SuccessCaseWithUnknownTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 20), TimeZone.UnknownTimeZone, 20)
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
            //        Date.of(1234, 1, 1),
            //        Time.of(0,0, BigDecimal.valueOf(0,0), TimeZone.UnknownTimezone, 0)
            // ),
            // ionTimestamp("1234-01-01T00:00:00-00:00").timestampValue
            // ),
            SuccessCaseWithUnknownTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Timestamp.of(
                    Date.of(1901, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), TimeZone.UnknownTimeZone, 0)
                ),
                ionTimestamp("1901-01-01T00:00:00-00:00").timestampValue
            ),
            // padding
            SuccessCaseWithUnknownTimeZone(
                "Padding zero",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(100000, 5), TimeZone.UnknownTimeZone, 5)
                ),
                ionTimestamp("1970-01-01T00:00:01.00000-00:00").timestampValue
            ),
        )

        private val successCaseWithNoTimeZone = listOf(
            SuccessCaseWithNoTimeZone(
                "Unix zero",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, null,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, null)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero - with fraction",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 3), null)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Unix zero with precision",
                1970, 1, 1, 0, 0, BigDecimal.ZERO, 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.ZERO, null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - truncate",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - up",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(999999, 5), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(10, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to minute",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 1, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to hour",
                1970, 1, 1, 0, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(1, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to day",
                1970, 1, 1, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 1, 2),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to month",
                1970, 1, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1970, 2, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Rounding - carry to year",
                1970, 12, 31, 23, 59, BigDecimal.valueOf(599, 1), 0,
                Timestamp.of(
                    Date.of(1971, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Large precision",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 20), 20,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 20), null, 20)
                ),
            ),
            // negative epoch
            // May be Ion has a bug?
            // Expected epoch in Millis : -23225270400000,
            // Actual epoch in Millis   : -23225875200000
            // SuccessCaseWithNoTimeZone(
            //    1234, 1, 1, 0,0, BigDecimal.valueOf(0,3), 0,
            //    Timestamp.of(
            //        Date.of(1234, 1, 1),
            //        Time.of(0,0, BigDecimal.valueOf(0,0), null, 0)
            // ),
            // ),
            SuccessCaseWithNoTimeZone(
                "Negative epoch",
                1901, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0,
                Timestamp.of(
                    Date.of(1901, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(0, 0), null, 0)
                ),
            ),
            SuccessCaseWithNoTimeZone(
                "Padding zeros",
                1970, 1, 1, 0, 0, BigDecimal.valueOf(1, 0), 5,
                Timestamp.of(
                    Date.of(1970, 1, 1),
                    Time.of(0, 0, BigDecimal.valueOf(100000, 5), null, 5)
                ),
            ),
        )

        private val equalsAndCompareToTestcase = listOf(
            // No Timezone
            // equals
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                true,
                EQUALS
            ),
            // different fraction
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(90L, 1), null, null)
                ),
                false,
                EQUALS
            ),
            // different precision
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, 10)
                ),
                false,
                EQUALS
            ),
            // larger
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 8, BigDecimal.valueOf(9L), null, null)
                ),
                false,
                MORE
            ),
            // Less
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), null, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(10, 8, BigDecimal.valueOf(9L), null, null)
                ),
                false,
                LESS
            ),

            // Known / Unknown time zone
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                true,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                false,
                EQUALS
            ),
            // fraction precision
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(90L, 1), TimeZone.UtcOffset.of(0), null)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(90L, 1), TimeZone.UnknownTimeZone, null)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                false,
                EQUALS
            ),
            // precision
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), 1)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, 1)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, 1)
                ),
                false,
                EQUALS
            ),
            // Time Zone Hour
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(8, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 0), null)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(8, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 0), null)
                ),
                false,
                EQUALS
            ),
            // Time zone hour minute
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(8, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 30), null)
                ),
                false,
                EQUALS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(8, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(1, 30), null)
                ),
                false,
                EQUALS
            ),

            // larger
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                false,
                MORE
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                false,
                MORE
            ),
            // less
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                false,
                LESS
            ),
            EqualsAndCompareToTest(
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(5, 38, BigDecimal.valueOf(9L), TimeZone.UnknownTimeZone, null)
                ),
                Timestamp.of(
                    Date.of(1234, 5, 6),
                    Time.of(7, 8, BigDecimal.valueOf(9L), TimeZone.UtcOffset.of(0), null)
                ),
                false,
                LESS
            ),
        )

        private val failedTestCases = listOf(
            // day
            FailedTest("Year more than for digits") { Date.of(12345, 1, 1) },
            FailedTest("Month more than two digits") { Date.of(1234, 123, 1) },
            FailedTest("Day more than two digits") { Date.of(1234, 12, 123) },
            FailedTest("Year less than zero") { Date.of(-1234, 12, 1) },
            FailedTest("Month less than zero") { Date.of(1234, -12, 1) },
            FailedTest("Day less than zero") { Date.of(1234, -12, -1) },
            FailedTest("Month > 12") { Date.of(1234, 13, 1) },
            FailedTest("Day > 31") { Date.of(1234, 1, 32) },
            FailedTest("not leap") { Date.of(2023, 2, 29) },
            // time
            FailedTest("hour 3 digits") { Time.of(123, 1, BigDecimal.ZERO, null, null) },
            FailedTest("minute 3 digits") { Time.of(12, 111, BigDecimal.ZERO, null, null) },
            FailedTest("whole second large than 2") { Time.of(12, 1, BigDecimal.valueOf(1000L, 1), null, null) },
            FailedTest("Timezone hour more than 3 digits") { Time.of(12, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(100, 0), null) },
            FailedTest("Time zone minutes more than 3 digits") { Time.of(12, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(1, 100), null) },
            FailedTest("Hour more than 24") { Time.of(25, 1, BigDecimal.ZERO, null, null) },
            FailedTest("Minute more than 60") { Time.of(12, 61, BigDecimal.ZERO, null, null) },
            FailedTest("Second more than 60") { Time.of(12, 1, BigDecimal.valueOf(61L), null, null) },

            // Timestamp comparison
            FailedTest("Compare with time zone and without time zone") {
                Timestamp.of(1234, 5, 6, 7, 8, BigDecimal.ZERO, null, 0)
                    .compareTo(Timestamp(1234, 5, 6, 7, 8, BigDecimal.ZERO, TimeZone.UtcOffset.of(0), 0))
            }
        )

        @JvmStatic
        fun allTestCases() = successCaseWithKnownTimeZone + successCaseWithUnKnownTimeZone + successCaseWithNoTimeZone + equalsAndCompareToTestcase + failedTestCases
    }
}
