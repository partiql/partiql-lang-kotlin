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
import com.amazon.ionelement.api.ionTimestamp
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.math.abs

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
                PType.TIMESTAMP -> ionTimestamp(this.localDateTime.toString() + "Z")
                PType.TIMESTAMPZ -> ionTimestamp(this.offsetDateTime.toString())
                PType.DATE -> ionString(this.localDate.toString()).withAnnotations("\$date")
                PType.TIME -> {
                    val time = this.localTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("hour", ionInt(time.hour.toLong())))
                    fields.add(field("minute", ionInt(time.minute.toLong())))
                    fields.add(field("second", ionDecimal(Decimal.valueOf(time.second.toBigDecimal()))))
                    ionStructOf(fields).withAnnotations("\$time")
                }
                PType.TIMEZ -> {
                    val offsetTime = this.offsetTime
                    val fields = mutableListOf<StructField>()
                    fields.add(field("hour", ionInt(offsetTime.hour.toLong())))
                    fields.add(field("minute", ionInt(offsetTime.minute.toLong())))
                    fields.add(field("second", ionDecimal(Decimal.valueOf(offsetTime.second.toBigDecimal()))))
                    val offset = offsetTime.offset
                    val totalSeconds = offset.totalSeconds
                    val timezoneHours = totalSeconds / 3600
                    val timezoneMinutes = (totalSeconds % 3600) / 60
                    fields.add(field("timezone_hour", ionInt(timezoneHours.toLong())))
                    fields.add(field("timezone_minute", ionInt(timezoneMinutes.toLong())))
                    ionStructOf(fields).withAnnotations("\$time")
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
