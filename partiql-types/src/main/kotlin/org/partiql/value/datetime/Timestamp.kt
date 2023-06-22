package org.partiql.value.datetime

import java.math.BigDecimal
import com.amazon.ion.Timestamp as TimestampIon

public data class Timestamp(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: BigDecimal,
    val timeZone: TimeZone?,
    val precision: Int?
) {
    public companion object {
        public fun of(date: Date, time: Time): Timestamp =
            Timestamp(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second,
                time.timeZone, time.precision
            )

        public fun of(ionTs: TimestampIon) {
            if (ionTs.localOffset == null) {
                Timestamp(
                    ionTs.year, ionTs.month, ionTs.day,
                    ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                    TimeZone.UnknownTimeZone,
                    null
                )
            } else {
                Timestamp(
                    ionTs.year, ionTs.month, ionTs.day,
                    ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                    TimeZone.UtcOffset.of(ionTs.localOffset),
                    null
                )
            }
        }
    }
}
