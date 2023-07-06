///*
// * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License").
// * You may not use this file except in compliance with the License.
// * A copy of the License is located at:
// *
// *      http://aws.amazon.com/apache2.0/
// *
// * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
// * language governing permissions and limitations under the License.
// */
//
//package org.partiql.value.datetime
//
//import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
//import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
//import java.math.BigDecimal
//import java.math.RoundingMode
//import java.time.temporal.ChronoField
//import kotlin.jvm.Throws
//
//// TODO: Further break this down to tow implementation, one with nanosecond and below precision
////  and the other with nano-decimalSecond and above precision, including arbitrary precision
////  The big decimal implementation is too slow and arguably for ion-compatibly reason only.
///**
// * This class is used to model both Time Without Time Zone type and Time With Time Zone Type.
// *
// * Informally, a data value of Time Without Time Zone represents a particular orientation of a clock
// * which will represent different instances of "time" based on the timezone.
// * a data value of Time With Time Zone represents an orientation of a clock attached with timezone offset.
// */
//public data class TimeX private constructor(
//    val hour: Int,
//    val minute: Int,
//    val decimalSecond: BigDecimal,
//    val timeZone: TimeZone?,
//    val precision: Int?
//) {
//    public companion object {
//        @JvmStatic
//        @Throws(DateTimeException::class)
//        public fun of(
//            hour: Int,
//            minute: Int,
//            decimalSecond: BigDecimal,
//            timeZone: TimeZone? = null,
//            precision: Int? = null
//        ): Time {
//            try {
//                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
//                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
//                // round down the decimalSecond to check
//                ChronoField.SECOND_OF_MINUTE.checkValidValue(decimalSecond.setScale(0, RoundingMode.DOWN).toLong())
//                val arbitraryTime = Time(hour, minute, decimalSecond, timeZone, null)
//                if (precision == null) { return arbitraryTime }
//                return arbitraryTime.toPrecision(precision)
//            } catch (e: java.time.DateTimeException) {
//                throw DateTimeException(e.localizedMessage, e)
//            }
//        }
//
//        public fun forSeconds(decimalSecond: BigDecimal, timeZone: TimeZone?, precision: Int? = null): Time {
//            val wholeSecond = decimalSecond.longValueExact()
//            val fraction = decimalSecond.minus(BigDecimal.valueOf(wholeSecond))
//            var total = wholeSecond
//            val hour = total / SECONDS_IN_HOUR
//            total -= hour * SECONDS_IN_HOUR
//            val minute = total / SECONDS_IN_MINUTE
//            total -= minute * SECONDS_IN_MINUTE
//            return of(hour.toInt(), minute.toInt(), fraction.plus(BigDecimal.valueOf(total)), timeZone, precision)
//        }
//    }
//
//    /**
//     * Counting the time escaped from midnight 00:00:00 in seconds ( fraction included)
//     */
//    val elapsedSecond: BigDecimal by lazy {
//        BigDecimal.valueOf(this.hour * SECONDS_IN_HOUR + this.minute * SECONDS_IN_MINUTE).plus(this.decimalSecond)
//    }
//
//    private fun toPrecision(precision: Int) =
//        when {
//            decimalSecond.scale() == precision -> this.copy(
//                hour = hour,
//                minute = minute,
//                decimalSecond = decimalSecond,
//                timeZone = timeZone,
//                precision = precision
//            )
//            decimalSecond.scale() < precision -> paddingToPrecision(precision)
//            else -> roundToPrecision(precision)
//        }
//
//    private fun paddingToPrecision(precision: Int) =
//        Time(
//            this.hour,
//            this.minute,
//            this.decimalSecond.setScale(precision),
//            this.timeZone,
//            precision
//        )
//
//    private fun roundToPrecision(precision: Int): Time {
//        var rounded = this.elapsedSecond.setScale(precision, RoundingMode.HALF_UP)
//        var newHours = 0
//        var newMinutes = 0
//        val secondsInHour = BigDecimal.valueOf(SECONDS_IN_HOUR)
//        val secondsInMin = BigDecimal.valueOf(SECONDS_IN_MINUTE)
//
//        if (rounded >= secondsInHour) {
//            val totalHours = rounded.divide(secondsInHour, 0, RoundingMode.DOWN)
//            rounded = rounded.subtract(totalHours.multiply(secondsInHour))
//            newHours = totalHours.intValueExact() % 24
//        }
//        if (rounded >= secondsInMin) {
//            val totalMinutes = rounded.divide(secondsInMin, 0, RoundingMode.DOWN)
//            rounded = rounded.subtract(totalMinutes.multiply(secondsInMin))
//            newMinutes = totalMinutes.intValueExact() % 60
//        }
//
//        return Time(newHours, newMinutes, rounded, this.timeZone, precision)
//    }
//
//    public fun atTimeZone(timeZone: TimeZone): Time = when (this.timeZone) {
//        TimeZone.UnknownTimeZone -> {
//            when (timeZone) {
//                TimeZone.UnknownTimeZone -> this
//                is TimeZone.UtcOffset -> this.copy(timeZone = TimeZone.UtcOffset.of(0)).atTimeZone(timeZone)
//            }
//        }
//
//        is TimeZone.UtcOffset -> {
//            val utc = this.plusMinutes(-this.timeZone.totalOffsetMinutes.toLong())
//            when (timeZone) {
//                TimeZone.UnknownTimeZone -> utc.copy(timeZone = timeZone)
//                is TimeZone.UtcOffset -> utc.plusMinutes(timeZone.totalOffsetMinutes.toLong()).copy(timeZone = timeZone)
//            }
//        }
//
//        null -> TODO("ERROR OUT")
//    }
//
//    public fun plusHour(hours: Long): Time =
//        forSeconds(this.elapsedSecond.plus(BigDecimal.valueOf(hours * SECONDS_IN_HOUR)), timeZone, precision)
//
//    public fun plusMinutes(minutes: Long): Time =
//        forSeconds(this.elapsedSecond.plus(BigDecimal.valueOf(minutes * SECONDS_IN_MINUTE)), timeZone, precision)
//
//    public fun plusSecond(seconds: Long): Time =
//        forSeconds(this.elapsedSecond.plus(BigDecimal.valueOf(seconds)), timeZone, precision)
//
//    public fun plusSecond(seconds: BigDecimal): Time = forSeconds(this.elapsedSecond.plus(seconds), timeZone, precision)
//}
