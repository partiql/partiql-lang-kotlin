package org.partiql.value.io

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.IonElement
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

/**
 * Make an arbitrary call on partiql value read
 * PartiQL generated annotation must be closed to the value
 *
 * For example:
 * In `IonForPartiQL` mode: if the input is $time::$day::$time{...}
 * The reader will attempt to create a time value
 */
class DatumIonReader(
    private val ionReader: IonReader,
    private val sourceDataFormat: DatumIonReaderBuilder.SourceDataFormat
) : AutoCloseable {
    private val MINUTES_PER_HOUR = 60
    private val INTERVAL_MAX_PRECISION = 9
    private val INTERVAL_MAX_FRACTIONAL_PRECISION = 9
    private val TIME_MAX_PRECISION = 9
    private enum class PARTIQL_ANNOTATION(val annotation: String) {
        MISSING_ANNOTATION("\$missing"),
        BAG_ANNOTATION("\$bag"),
        DATE_ANNOTATION("\$date"),
        TIME_ANNOTATION("\$time"),
        TIMESTAMP_ANNOTATION("\$timestamp"),
        INTERVAL_YM_ANNOTATION("\$interval_ym"),
        INTERVAL_DT_ANNOTATION("\$interval_dt"),
        GRAPH_ANNOTATION("\$graph")
    }

    @Throws(IOException::class)
    fun read(): Datum {
        val value = if (ionReader.next() != null) {
            fromIon(ionReader)
        } else {
            throw IOException("End of File.")
        }
        return value
    }

    override fun close() {
        ionReader.close()
    }

    private fun fromIon(reader: IonReader): Datum {
        return when (sourceDataFormat) {
            DatumIonReaderBuilder.SourceDataFormat.IonGeneric -> fromIonGeneric(reader)
            DatumIonReaderBuilder.SourceDataFormat.IonForPartiQL -> fromIonForPartiQL(reader)
        }
    }

    /**
     * This is specifically used when the value is null.
     */
    private fun fromIonType(type: IonType): PType {
        return when (type) {
            IonType.NULL -> PType.unknown()
            IonType.BOOL -> PType.bool()
            IonType.INT -> PType.bigint()
            IonType.FLOAT -> PType.doublePrecision()
            IonType.DECIMAL -> PType.decimal(38, 0)
            IonType.TIMESTAMP -> PType.timestamp(6)
            IonType.STRING, IonType.SYMBOL -> PType.string()
            IonType.CLOB -> PType.clob(Long.MAX_VALUE)
            IonType.BLOB -> PType.blob(Int.MAX_VALUE)
            IonType.LIST, IonType.SEXP -> PType.array()
            IonType.STRUCT -> PType.struct()
            IonType.DATAGRAM -> error("Datagram is not supported")
        }
    }

    private fun fromIonGeneric(reader: IonReader): Datum {
        val type = reader.type
        if (reader.isNullValue) {
            return Datum.nullValue(fromIonType(type))
        }
        return when (type!!) {
            IonType.NULL -> Datum.nullValue() // TODO: Annotations
            IonType.BOOL -> Datum.bool(reader.booleanValue())
            IonType.INT -> {
                val d = reader.bigIntegerValue().toBigDecimal()
                if (d.scale() != 0) {
                    Datum.decimal(d, d.precision(), d.scale())
                }
                if (d.precision() > 38) {
                    Datum.decimal(d, 38, d.scale())
                }
                try {
                    Datum.integer(d.intValueExact())
                } catch (e: ArithmeticException) {
                    Datum.bigint(d.longValueExact())
                }
            }
            IonType.FLOAT -> Datum.doublePrecision(reader.doubleValue())
            IonType.DECIMAL -> {
                val d = reader.bigDecimalValue()
                Datum.decimal(d, d.precision(), d.scale())
            }
            IonType.TIMESTAMP -> {
                val ts = reader.timestampValue()
                val tz = when (ts.localOffset) {
                    null -> ZoneOffset.UTC
                    else -> {
                        val offsetHour: Int = ts.localOffset / MINUTES_PER_HOUR
                        val offsetMinute: Int = ts.localOffset % MINUTES_PER_HOUR
                        ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute)
                    }
                }
                // [0-59].000_000_000
                val ds = ts.decimalSecond
                val second: Int = ds.toInt()
                val nanoOfSecond: Int = ds.remainder(BigDecimal.ONE).movePointRight(TIME_MAX_PRECISION).toInt()
                // date/time pair
                val date = LocalDate.of(ts.year, ts.month, ts.day)
                val time = LocalTime.of(ts.hour, ts.minute, second, nanoOfSecond)
                return Datum.timestampz(OffsetDateTime.of(date, time, tz), TIME_MAX_PRECISION)
            }
            IonType.STRING, IonType.SYMBOL -> Datum.string(reader.stringValue())
            IonType.CLOB -> Datum.clob(reader.newBytes())
            IonType.BLOB -> Datum.blob(reader.newBytes())
            IonType.LIST, IonType.SEXP -> {
                reader.stepIn()
                val elements = mutableListOf<Datum>().also { elements ->
                    reader.loadEachValue {
                        elements.add(fromIon(reader))
                    }
                }
                reader.stepOut()
                Datum.array(elements)
            }
            IonType.STRUCT -> {
                reader.stepIn()
                val elements = mutableListOf<Field>().also { elements ->
                    reader.loadEachValue {
                        elements.add(Field.of(reader.fieldName, fromIon(reader)))
                    }
                }
                reader.stepOut()
                Datum.struct(elements)
            }
            IonType.DATAGRAM -> throw IllegalArgumentException("Datagram not supported")
        }
    }

    private fun checkAnnotations(type: IonType, annotations: List<String>) {
        when (getPartiQLReservedAnnotation(annotations)) {
            null -> return
            PARTIQL_ANNOTATION.MISSING_ANNOTATION -> assert(type == IonType.NULL)
            PARTIQL_ANNOTATION.BAG_ANNOTATION -> assert(type == IonType.LIST)
            PARTIQL_ANNOTATION.DATE_ANNOTATION -> assert(type == IonType.STRUCT)
            PARTIQL_ANNOTATION.TIME_ANNOTATION -> assert(type == IonType.STRUCT)
            PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> assert(type == IonType.STRUCT)
            PARTIQL_ANNOTATION.INTERVAL_YM_ANNOTATION -> assert(type == IonType.STRUCT)
            PARTIQL_ANNOTATION.INTERVAL_DT_ANNOTATION -> assert(type == IonType.STRUCT)
            PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> assert(type == IonType.STRUCT)
        }
    }

    // We have to check if an inappropriate annotation exists
    // for example: $missing::1
    // this makes the code not very easy to maintain as we potentially can add more reserved annotation
    // I made the deliberate decision to list out all branches so we don't accidentally forget one in the future...
    private fun fromIonForPartiQL(reader: IonReader): Datum {
        val annotations = reader.typeAnnotations.toList()
        val lastAnnotation = getPartiQLReservedAnnotation(annotations)
        val type = reader.type!!
        checkAnnotations(type, annotations)
        if (type == IonType.NULL) {
            return when (lastAnnotation) {
                PARTIQL_ANNOTATION.MISSING_ANNOTATION -> Datum.missing()
                PARTIQL_ANNOTATION.BAG_ANNOTATION -> Datum.nullValue(PType.bag())
                PARTIQL_ANNOTATION.DATE_ANNOTATION -> Datum.nullValue(PType.date())
                PARTIQL_ANNOTATION.TIME_ANNOTATION -> Datum.nullValue(PType.time(6))
                PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> Datum.nullValue(PType.timestamp(6))
                PARTIQL_ANNOTATION.INTERVAL_YM_ANNOTATION -> Datum.nullValue(PType.intervalYearMonth(INTERVAL_MAX_PRECISION))
                PARTIQL_ANNOTATION.INTERVAL_DT_ANNOTATION -> Datum.nullValue(PType.intervalDaySecond(INTERVAL_MAX_PRECISION, INTERVAL_MAX_FRACTIONAL_PRECISION))
                PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> TODO("Graph not yet implemented")
                null -> Datum.nullValue()
            }
        }
        if (reader.isNullValue) {
            return Datum.nullValue(fromIonType(type))
        }
        return when (type) {
            IonType.BOOL,
            IonType.FLOAT,
            IonType.INT,
            IonType.TIMESTAMP,
            IonType.STRING,
            IonType.SYMBOL,
            IonType.DECIMAL,
            IonType.CLOB,
            IonType.BLOB -> fromIonGeneric(reader)
            IonType.LIST -> {
                if (lastAnnotation == PARTIQL_ANNOTATION.BAG_ANNOTATION) {
                    reader.stepIn()
                    val elements = mutableListOf<Datum>().also { elements ->
                        reader.loadEachValue {
                            elements.add(fromIon(reader))
                        }
                    }
                    reader.stepOut()
                    Datum.bag(elements)
                } else {
                    reader.stepIn()
                    val elements = mutableListOf<Datum>().also { elements ->
                        reader.loadEachValue {
                            elements.add(fromIon(reader))
                        }
                    }
                    reader.stepOut()
                    Datum.array(elements)
                }
            }

            IonType.SEXP -> {
                reader.stepIn()
                val elements = mutableListOf<Datum>().also { elements ->
                    reader.loadEachValue {
                        elements.add(fromIon(reader))
                    }
                }
                reader.stepOut()
                Datum.array(elements)
            }

            IonType.STRUCT -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> {
                        reader.stepIn()
                        val year = getRequiredFieldName(reader, "year")
                        val month = getRequiredFieldName(reader, "month")
                        val day = getRequiredFieldName(reader, "day")
                        if (reader.next() != null) {
                            throw IllegalArgumentException("excess field in struct")
                        }
                        reader.stepOut()
                        Datum.date(LocalDate.of(year.int, month.int, day.int))
                    }
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> {
                        reader.stepIn()
                        val hour = getRequiredFieldName(reader, "hour")
                        val minute = getRequiredFieldName(reader, "minute")
                        val second = getRequiredFieldName(reader, "second")
                        val offset: Datum? = getOptionalFieldName(reader, "offset")
                        // check remaining
                        if (reader.next() != null) {
                            throw IllegalArgumentException("excess field in struct")
                        }
                        reader.stepOut()
                        val offsetTz = when {
                            offset == null || offset.isNull -> ZoneOffset.UTC
                            else -> ZoneOffset.ofHoursMinutes(offset.int / MINUTES_PER_HOUR, offset.int % MINUTES_PER_HOUR)
                        }
                        // calculate second/nanos
                        val ds = second.bigDecimal
                        val seconds: Int = ds.toInt()
                        val nanoOfSecond: Int = ds.remainder(BigDecimal.ONE).movePointRight(TIME_MAX_PRECISION).toInt()

                        Datum.timez(OffsetTime.of(hour.int, minute.int, seconds, nanoOfSecond, offsetTz), TIME_MAX_PRECISION)
                    }
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> {
                        reader.stepIn()
                        // check fields
                        val year = getRequiredFieldName(reader, "year")
                        val month = getRequiredFieldName(reader, "month")
                        val day = getRequiredFieldName(reader, "day")
                        val hour = getRequiredFieldName(reader, "hour")
                        val minute = getRequiredFieldName(reader, "minute")
                        val second = getRequiredFieldName(reader, "second")
                        val offset: Datum? = getOptionalFieldName(reader, "offset")
                        // check remaining
                        if (reader.next() != null) {
                            throw IllegalArgumentException("excess field in struct")
                        }
                        reader.stepOut()
                        val offsetTz = when {
                            offset == null || offset.isNull -> ZoneOffset.UTC
                            else -> ZoneOffset.ofHoursMinutes(offset.int / MINUTES_PER_HOUR, offset.int % MINUTES_PER_HOUR)
                        }
                        // calculate second/nanos
                        val ds = second.bigDecimal
                        val seconds: Int = ds.toInt()
                        val nanoOfSecond: Int = ds.remainder(BigDecimal.ONE).movePointRight(TIME_MAX_PRECISION).toInt()
                        // date/time
                        val date = LocalDate.of(year.int, month.int, day.int)
                        val time = LocalTime.of(hour.int, minute.int, seconds, nanoOfSecond)
                        Datum.timestampz(OffsetDateTime.of(date, time, offsetTz), TIME_MAX_PRECISION)
                    }
                    PARTIQL_ANNOTATION.INTERVAL_YM_ANNOTATION -> {
                        reader.stepIn()
                        var sign: String? = null
                        var years = 0
                        var months = 0
                        while (reader.next() != null) {
                            when (reader.fieldName) {
                                "sign" -> sign = reader.stringValue()
                                "years" -> years = reader.intValue()
                                "months" -> months = reader.intValue()
                                else -> throw IllegalArgumentException("unexpected field: ${reader.fieldName}")
                            }
                        }
                        reader.stepOut()
                        val signMultiplier = if (sign == "-") -1 else 1
                        when {
                            years < 0 -> throw IllegalArgumentException("years must be non-negative, got: $years")
                            months < 0 -> throw IllegalArgumentException("months must be non-negative, got: $months")
                        }
                        Datum.intervalYearMonth(signMultiplier * years, signMultiplier * months, INTERVAL_MAX_PRECISION)
                    }
                    PARTIQL_ANNOTATION.INTERVAL_DT_ANNOTATION -> {
                        reader.stepIn()
                        var sign: String? = null
                        var days = 0
                        var hours = 0
                        var minutes = 0
                        var seconds = 0
                        var nanos = 0
                        while (reader.next() != null) {
                            when (reader.fieldName) {
                                "sign" -> sign = reader.stringValue()
                                "days" -> days = reader.intValue()
                                "hours" -> hours = reader.intValue()
                                "minutes" -> minutes = reader.intValue()
                                "seconds" -> seconds = reader.intValue()
                                "nanos" -> nanos = reader.intValue()
                                else -> throw IllegalArgumentException("unexpected field: ${reader.fieldName}")
                            }
                        }
                        reader.stepOut()
                        val signMultiplier = if (sign == "-") -1 else 1
                        when {
                            days < 0 -> throw IllegalArgumentException("days must be non-negative, got: $days")
                            hours < 0 -> throw IllegalArgumentException("hours must be non-negative, got: $hours")
                            minutes < 0 -> throw IllegalArgumentException("minutes must be non-negative, got: $minutes")
                            seconds < 0 -> throw IllegalArgumentException("seconds must be non-negative, got: $seconds")
                            nanos < 0 -> throw IllegalArgumentException("nanos must be non-negative, got: $nanos")
                        }
                        Datum.intervalDaySecond(
                            signMultiplier * days,
                            signMultiplier * hours,
                            signMultiplier * minutes,
                            signMultiplier * seconds,
                            signMultiplier * nanos,
                            INTERVAL_MAX_PRECISION,
                            INTERVAL_MAX_FRACTIONAL_PRECISION,
                        )
                    }
                    null -> fromIonGeneric(reader)
                    else -> error("Unsupported annotation.")
                }
            }

            IonType.DATAGRAM -> throw IllegalArgumentException("Datagram not supported")
            IonType.NULL -> error("This should not have been reached.")
        }
    }

    private fun <T> IonReader.loadEachValue(block: () -> T) {
        while (this.next() != null) {
            block()
        }
    }

    private fun getPartiQLReservedAnnotation(partiqlAnnotation: List<String>) =
        partiqlAnnotation.lastOrNull()?.let { lastAnnotation ->
            PARTIQL_ANNOTATION.values().find { it.annotation == lastAnnotation }
        }

    private fun getRequiredFieldName(reader: IonReader, expectedField: String): Datum {
        if (reader.next() == null) {
            throw IllegalArgumentException("missing $expectedField field")
        }
        if (reader.fieldName == expectedField) {
            val v = fromIon(reader)
            return v
        } else {
            throw IllegalArgumentException("expect $expectedField, but received ${reader.fieldName}")
        }
    }

    private fun getOptionalFieldName(reader: IonReader, expectedField: String): Datum? {
        if (reader.next() == null) {
            throw IllegalArgumentException("missing $expectedField field")
        }
        if (reader.fieldName == expectedField) {
            val v = fromIon(reader)
            return v
        } else {
            return null
        }
    }
}

/**
 * @param sourceDataFormat  The source data format of the Ion data; if not specified, defaults to [SourceDataFormat.IonForPartiQL].
 * @param ionReaderBuilder The builder for creating the [IonReader].
 */
public class DatumIonReaderBuilder private constructor(
    private var sourceDataFormat: SourceDataFormat = SourceDataFormat.IonForPartiQL,
    private var ionReaderBuilder: IonReaderBuilder,
) {
    public enum class SourceDataFormat {
        /**
         * The ion value annotations are always treated as annotations on PartiQL value.
         * For example, $missing::null will be treated as nullValue(annotations = ["missing"])
         */
        IonGeneric,

        /**
         * We examine the **last** annotation before convert to PartiQL Value.
         * If the annotation is PartiQL reserved, we validate Semantics and the constructed PartiQL value may be different.
         * For example:
         *   - $missing::null will be treated as missingValue(annotations = [])
         *   - a::b::$missing:null will be treated as missingValue(annotations = ["a", "b"])
         *   - a::$missing::b::null will be treated as nullValue(annotation = ["a", "$missing", "b"]
         *   - $missing::1 will cause an exception.
         */
        IonForPartiQL
    }

    public companion object {

        /**
         * Creates a [DatumIonReaderBuilder] with the default settings.
         * The default source data format is [SourceDataFormat.IonForPartiQL] and the default [IonReaderBuilder] is [IonReaderBuilder.standard]
         */
        @JvmStatic
        public fun standard(): DatumIonReaderBuilder = DatumIonReaderBuilder(
            sourceDataFormat = SourceDataFormat.IonForPartiQL,
            ionReaderBuilder = IonReaderBuilder.standard()
        )
    }

    public fun build(inputStream: InputStream): DatumIonReader =
        DatumIonReader(
            ionReader = ionReaderBuilder.build(inputStream),
            sourceDataFormat = sourceDataFormat
        )

    public fun build(ionElement: IonElement): DatumIonReader {
        val out = ByteArrayOutputStream()
        val writer = IonTextWriterBuilder.standard().build(out)
        ionElement.writeTo(writer)
        val input = ByteArrayInputStream(out.toByteArray())
        return build(input)
    }

    public fun sourceDataFormat(sourceDataFormat: SourceDataFormat): DatumIonReaderBuilder = this.apply {
        this.sourceDataFormat = sourceDataFormat
    }

    public fun ionReaderBuilder(ionReaderBuilder: IonReaderBuilder): DatumIonReaderBuilder = this.apply {
        this.ionReaderBuilder = ionReaderBuilder
    }
}
