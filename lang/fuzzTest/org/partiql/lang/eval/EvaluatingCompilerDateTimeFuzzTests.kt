package org.partiql.lang.eval

import com.amazon.ion.IonStruct
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.ArgumentsProviderBase
import java.math.RoundingMode
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.random.Random

internal const val HOURS_PER_DAY = 24
internal const val MINUTES_PER_HOUR = 60
internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val NANOS_PER_SECOND = 1000000000
internal const val MAX_PRECISION_FOR_TIME = 9

class EvaluatingCompilerDateTimeFuzzTests : EvaluatorTestBase() {

    abstract class RandomTestsProvider() : ArgumentsProviderBase() {
        private val randomTestsSize = 50000
        private val randomGenerator = generateRandomSeed()

        private fun generateRandomSeed(): Random {
            val seed = Random.nextInt()
            println("Randomly generated seed is $seed. Use this to reproduce failures in dev environment.")
            return Random(seed)
        }

        private fun Random.nextTime(withPrecision: Boolean = false, withTimezone: Boolean = false): TimeForValidation {
            val hour = nextInt(24)
            val minute = nextInt(60)
            val second = nextInt(60)
            val nano = nextInt(999999999)
            val precision = if (withPrecision) {
                nextInt(10)
            } else {
                val timeStr = Time.of(hour, minute, second, nano, 9).toString()
                timeStr.split(".")[1].length
            }
            val timezoneMinutes = if (withTimezone) {
                nextInt(-1080, 1081)
            } else {
                null
            }
            return TimeForValidation(hour, minute, second, nano, precision, timezoneMinutes)
        }
        protected val RANDOM_TIMES = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = false,
                withTimezone = false
            )
        }
        protected val RANDOM_TIMES_WITH_PRECISION = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = true,
                withTimezone = false
            )
        }
        protected val RANDOM_TIMES_WITH_TIMEZONE = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = false,
                withTimezone = true
            )
        }
        protected val RANDOM_TIMES_WITH_PRECISION_AND_TIMEZONE = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = true,
                withTimezone = true
            )
        }

        class RandomTimesAndRandomTimesWithTimezone() : RandomTestsProvider() {
            override fun getParameters(): List<Any> = super.RANDOM_TIMES + super.RANDOM_TIMES_WITH_TIMEZONE
        }

        class RandomTimesWithPrecisionAndRandomTimesWithPrecisionAndTimezone() : RandomTestsProvider() {
            override fun getParameters(): List<Any> = super.RANDOM_TIMES_WITH_PRECISION + super.RANDOM_TIMES_WITH_PRECISION_AND_TIMEZONE
        }
    }

    private fun secondsWithPrecision(time: TimeForValidation) =
        ion.newDecimal(time.second.toBigDecimal() + time.nano.toBigDecimal().divide(NANOS_PER_SECOND.toBigDecimal()).setScale(time.precision, RoundingMode.HALF_UP))

    private fun assertEqualsIonTimeStruct(actual: IonStruct, expectedTime: TimeForValidation) {
        assertEquals(ion.newInt(expectedTime.hour), actual["hour"])
        assertEquals(ion.newInt(expectedTime.minute), actual["minute"])
        assertEquals(secondsWithPrecision(expectedTime), actual["second"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.div(MINUTES_PER_HOUR)), actual["timezone_hour"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.rem(MINUTES_PER_HOUR)), actual["timezone_minute"])
    }

    data class TimeTestCase(
        val query: String,
        val expected: String,
        val expectedTime: TimeForValidation? = null,
        val compileOptionsBlock: CompileOptions.Builder.() -> Unit
    )

    data class TimeForValidation(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val nano: Int = 0,
        val precision: Int,
        val tz_minutes: Int? = null
    ) {
        fun expectedTimeString(withTimeZone: Boolean): String {
            val timezoneMinutes = when (withTimeZone) {
                true -> tz_minutes ?: ZoneOffset.UTC.totalSeconds / SECONDS_PER_MINUTE
                else -> null
            }
            return Time.of(hour, minute, second, nano, precision, timezoneMinutes).toString()
        }

        override fun toString(): String {
            val hourStr = hour.toString().padStart(2, '0')
            val minStr = minute.toString().padStart(2, '0')
            val secStr = second.toString().padStart(2, '0')
            val nanoStr = nano.toString().padStart(9, '0')
            val timezoneStr = tz_minutes?.let {
                "" +
                    (if (it >= 0) "+" else "-") +
                    (it.absoluteValue / 60).toString().padStart(2, '0') +
                    ":" +
                    (it.absoluteValue % 60).toString().padStart(2, '0')
            } ?: ""
            return "$hourStr:$minStr:$secStr.$nanoStr$timezoneStr"
        }
    }

    @ParameterizedTest
    @ArgumentsSource(RandomTestsProvider.RandomTimesAndRandomTimesWithTimezone::class)
    fun testRandomTimes(time: TimeForValidation) {
        val query = "TIME '$time'"
        val expected = "TIME '${time.expectedTimeString(withTimeZone = false)}'"
        runEvaluatorTestCase(
            query = query,
            expectedResult = expected,
            expectedResultFormat = ExpectedResultFormat.STRING
        )
    }

    @ParameterizedTest
    @ArgumentsSource(RandomTestsProvider.RandomTimesWithPrecisionAndRandomTimesWithPrecisionAndTimezone::class)
    fun testRandomTimesWithPrecision(time: TimeForValidation) {
        val query = "TIME (${time.precision}) '$time'"
        val expected = "TIME '${time.expectedTimeString(withTimeZone = false)}'"
        runEvaluatorTestCase(
            query = query,
            expectedResult = expected,
            expectedResultFormat = ExpectedResultFormat.STRING
        )
    }

    @ParameterizedTest
    @ArgumentsSource(RandomTestsProvider.RandomTimesAndRandomTimesWithTimezone::class)
    fun testRandomTimesWithTimezone(time: TimeForValidation) {
        val query = "TIME WITH TIME ZONE '$time'"
        val expected = "TIME WITH TIME ZONE '${time.expectedTimeString(withTimeZone = true)}'"
        runEvaluatorTestCase(
            query = query,
            expectedResult = expected,
            expectedResultFormat = ExpectedResultFormat.STRING
        )
    }

    @ParameterizedTest
    @ArgumentsSource(RandomTestsProvider.RandomTimesWithPrecisionAndRandomTimesWithPrecisionAndTimezone::class)
    fun testRandomTimesWithPrecisionAndTimezone(time: TimeForValidation) {
        val query = "TIME (${time.precision}) WITH TIME ZONE '$time'"
        val expected = "TIME WITH TIME ZONE '${time.expectedTimeString(withTimeZone = true)}'"
        runEvaluatorTestCase(
            query = query,
            expectedResult = expected,
            expectedResultFormat = ExpectedResultFormat.STRING
        )
    }
}
