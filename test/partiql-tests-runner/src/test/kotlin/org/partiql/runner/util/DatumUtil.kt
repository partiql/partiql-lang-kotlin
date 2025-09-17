package org.partiql.runner.util

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.math.abs

const val SecondPerMinute = 60
const val nanoPerSecond = 1000000000L

// For error display only
internal fun Datum.toIonElement(): IonElement {
    return when {
        this.isMissing -> ionNull().withAnnotations("\$missing")
        this.isNull -> ionNull()
        else -> {
            when (this.type.code()) {
                PType.BOOL -> ionBool(this.boolean)
                PType.INTEGER -> ionInt(this.int.toLong())
                PType.BIGINT -> ionInt(this.long)
                PType.STRING -> ionString(this.string)
                PType.DECIMAL -> ionDecimal(Decimal.valueOf(this.bigDecimal))
                PType.DOUBLE -> ionFloat(this.double)
                PType.REAL -> ionFloat(this.float.toDouble())
                PType.DATE -> ionString(this.localDate.toString()).withAnnotations("\$date")
                PType.TIME -> {
                    val time = this.localTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("hour", ionInt(time.hour.toLong())))
                    fields.add(field("minute", ionInt(time.minute.toLong())))
                    val totalSeconds = time.second.toBigDecimal() + time.nano.toBigDecimal().divide(nanoPerSecond.toBigDecimal())
                    fields.add(field("second", ionDecimal(Decimal.valueOf(totalSeconds))))
                    fields.add(field("offset", ionInt(0L)))
                    ionStructOf(fields).withAnnotations("\$time")
                }
                PType.TIMEZ -> {
                    val offsetTime = this.offsetTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("hour", ionInt(offsetTime.hour.toLong())))
                    fields.add(field("minute", ionInt(offsetTime.minute.toLong())))
                    val totalSeconds = offsetTime.second.toBigDecimal() + offsetTime.nano.toBigDecimal().divide(nanoPerSecond.toBigDecimal())
                    fields.add(field("second", ionDecimal(Decimal.valueOf(totalSeconds))))
                    val offset = offsetTime.offset.totalSeconds / SecondPerMinute
                    fields.add(field("offset", ionInt(offset.toLong())))
                    ionStructOf(fields).withAnnotations("\$time")
                }
                PType.TIMESTAMP -> {
                    val timestamp = this.localDateTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("year", ionInt(timestamp.year.toLong())))
                    fields.add(field("month", ionInt(timestamp.monthValue.toLong())))
                    fields.add(field("day", ionInt(timestamp.dayOfMonth.toLong())))
                    fields.add(field("hour", ionInt(timestamp.hour.toLong())))
                    fields.add(field("minute", ionInt(timestamp.minute.toLong())))
                    val totalSeconds = timestamp.second.toBigDecimal() + timestamp.nano.toBigDecimal().divide(nanoPerSecond.toBigDecimal())
                    fields.add(field("second", ionDecimal(Decimal.valueOf(totalSeconds))))
                    fields.add(field("offset", ionInt(0L)))
                    ionStructOf(fields).withAnnotations("\$timestamp")
                }
                PType.TIMESTAMPZ -> {
                    val offsetDateTime = this.offsetDateTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("year", ionInt(offsetDateTime.year.toLong())))
                    fields.add(field("month", ionInt(offsetDateTime.monthValue.toLong())))
                    fields.add(field("day", ionInt(offsetDateTime.dayOfMonth.toLong())))
                    fields.add(field("hour", ionInt(offsetDateTime.hour.toLong())))
                    fields.add(field("minute", ionInt(offsetDateTime.minute.toLong())))
                    val totalSeconds = offsetDateTime.second.toBigDecimal() + offsetDateTime.nano.toBigDecimal().divide(nanoPerSecond.toBigDecimal())
                    fields.add(field("second", ionDecimal(Decimal.valueOf(totalSeconds))))
                    val offset = offsetDateTime.offset.totalSeconds / SecondPerMinute
                    fields.add(field("offset", ionInt(offset.toLong())))
                    ionStructOf(fields).withAnnotations("\$timestamp")
                }
                PType.INTERVAL_YM -> {
                    val fields = mutableListOf<StructField>()
                    val totalMonths = this.totalMonths
                    if (totalMonths < 0) {
                        fields.add(field("sign", ionString("-")))
                    } else {
                        fields.add(field("sign", ionString("+")))
                    }
                    fields.add(field("years", ionInt(abs(this.years).toLong())))
                    fields.add(field("months", ionInt(abs(this.months).toLong())))
                    ionStructOf(fields).withAnnotations("\$interval_ym")
                }
                PType.INTERVAL_DT -> {
                    val fields = mutableListOf<StructField>()
                    val totalSeconds = this.totalSeconds
                    if (totalSeconds < 0 || this.nanos < 0) {
                        fields.add(field("sign", ionString("-")))
                    } else {
                        fields.add(field("sign", ionString("+")))
                    }
                    fields.add(field("days", ionInt(abs(this.days).toLong())))
                    fields.add(field("hours", ionInt(abs(this.hours).toLong())))
                    fields.add(field("minutes", ionInt(abs(this.minutes).toLong())))
                    fields.add(field("seconds", ionInt(abs(this.seconds).toLong())))
                    fields.add(field("nanos", ionInt(abs(this.nanos).toLong())))
                    ionStructOf(fields).withAnnotations("\$interval_dt")
                }
                PType.ARRAY -> {
                    val elements = mutableListOf<IonElement>()
                    for (element in this) {
                        elements.add(element.toIonElement())
                    }
                    ionListOf(elements)
                }
                PType.BAG -> {
                    val elements = mutableListOf<IonElement>()
                    for (element in this) {
                        elements.add(element.toIonElement())
                    }
                    ionListOf(elements).withAnnotations("\$bag")
                }
                PType.STRUCT, PType.ROW -> {
                    val fields = mutableListOf<StructField>()
                    val fieldIterator = this.fields
                    while (fieldIterator.hasNext()) {
                        val field = fieldIterator.next()
                        fields.add(field(field.name, field.value.toIonElement()))
                    }
                    ionStructOf(fields)
                }
                PType.VARIANT -> {
                    val lowered = this.lower()
                    lowered.toIonElement().withAnnotations("\$ion")
                }
                else -> ionString(this.toString())
            }
        }
    }
}
