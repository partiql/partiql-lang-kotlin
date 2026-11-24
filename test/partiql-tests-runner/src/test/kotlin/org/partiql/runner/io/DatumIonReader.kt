package org.partiql.value.io

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.IonElement
import org.partiql.runner.util.JSON_ANNOTATION
import org.partiql.runner.util.SecondPerMinute
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry
import org.partiql.spi.value.Field
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val JSON_MAX_DECIMAL_DIGITS = 38
    private val JSON_MAX_OFFSET_MINUTES = 18 * 60 // the largest offset ZoneOffset allows
    private enum class PARTIQL_ANNOTATION(val annotation: String) {
        MISSING_ANNOTATION("\$missing"),
        BAG_ANNOTATION("\$bag"),
        MAP_ANNOTATION("\$map"),
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
            IonType.CLOB -> PType.clob(Int.MAX_VALUE)
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
            PARTIQL_ANNOTATION.MAP_ANNOTATION -> assert(type == IonType.LIST)
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
        if (JSON_ANNOTATION in annotations) {
            if (annotations.first() != JSON_ANNOTATION) {
                throw invalidJson("$JSON_ANNOTATION must be the outermost annotation, got $annotations")
            }
            return readJson(reader, annotations.drop(1))
        }
        val partiQLAnnotation = getPartiQLReservedAnnotation(annotations)
        val type = reader.type!!
        checkAnnotations(type, annotations)
        if (type == IonType.NULL) {
            return when (partiQLAnnotation) {
                PARTIQL_ANNOTATION.MISSING_ANNOTATION -> Datum.missing()
                PARTIQL_ANNOTATION.BAG_ANNOTATION -> Datum.nullValue(PType.bag())
                PARTIQL_ANNOTATION.MAP_ANNOTATION -> {
                    val (keyType, valueType) = getMapTypes(annotations)
                    Datum.nullValue(PType.map(keyType, valueType))
                }
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
                if (partiQLAnnotation == PARTIQL_ANNOTATION.BAG_ANNOTATION) {
                    reader.stepIn()
                    val elements = mutableListOf<Datum>().also { elements ->
                        reader.loadEachValue {
                            elements.add(fromIon(reader))
                        }
                    }
                    reader.stepOut()
                    Datum.bag(elements)
                } else if (partiQLAnnotation == PARTIQL_ANNOTATION.MAP_ANNOTATION) {
                    val (keyType, valueType) = getMapTypes(annotations)
                    reader.stepIn()
                    val entries = mutableListOf<Entry>()
                    reader.loadEachValue {
                        // Each element is a list [key, value]
                        reader.stepIn()
                        val pair = mutableListOf<Datum>()
                        while (reader.next() != null) {
                            pair.add(fromIon(reader))
                        }
                        reader.stepOut()
                        if (pair.size != 2) {
                            throw IllegalArgumentException("MAP entry must have exactly 2 elements [key, value], but found ${pair.size}")
                        }
                        entries.add(Entry.of(pair[0], pair[1]))
                    }
                    reader.stepOut()
                    Datum.map(keyType, valueType, entries)
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
                when (partiQLAnnotation) {
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

    /**
     * Reads a value annotated with $json as an SQL/JSON item, following docs/partiql-tests-schema-proposal.md in
     * partiql-tests. [annotations] are the annotations after $json: none, or one of $date, $time and $timestamp.
     */
    @Suppress("DEPRECATION")
    private fun readJson(reader: IonReader, annotations: List<String>): Datum {
        if (JSON_ANNOTATION in annotations) {
            throw invalidJson("a nested $JSON_ANNOTATION is not allowed")
        }
        val datetime = when {
            annotations.isEmpty() -> null
            annotations.size == 1 && annotations[0] in JSON_DATETIME_ANNOTATIONS -> annotations[0]
            else -> throw invalidJson("the annotations $annotations are not allowed inside $JSON_ANNOTATION")
        }
        val type = reader.type!!
        if (datetime != null) {
            if (type != IonType.STRUCT || reader.isNullValue) {
                throw invalidJson("$datetime must annotate a struct, got ${if (reader.isNullValue) "null" else type.name.lowercase()}")
            }
            return Datum.jsonScalar(readJsonDatetime(reader, datetime))
        }
        if (reader.isNullValue) {
            if (type != IonType.NULL) {
                throw invalidJson("only an untyped null is allowed, got null.${type.name.lowercase()}")
            }
            return Datum.jsonNull()
        }
        return when (type) {
            IonType.BOOL -> Datum.jsonScalar(Datum.bool(reader.booleanValue()))
            IonType.INT -> {
                val v = reader.bigIntegerValue()
                val n = when {
                    v.bitLength() < Int.SIZE_BITS -> Datum.integer(v.toInt())
                    v.bitLength() < Long.SIZE_BITS -> Datum.bigint(v.toLong())
                    else -> throw invalidJson("an integer too large for BIGINT; write it as an Ion decimal, with a trailing period")
                }
                Datum.jsonScalar(n)
            }
            IonType.DECIMAL -> Datum.jsonScalar(readJsonDecimal(reader))
            IonType.FLOAT -> {
                val v = reader.doubleValue()
                if (v.isNaN() || v.isInfinite()) {
                    throw invalidJson("$v is not allowed, because JSON text can't write it")
                }
                Datum.jsonScalar(Datum.doublePrecision(v))
            }
            IonType.STRING -> Datum.jsonScalar(Datum.string(reader.stringValue()))
            IonType.LIST -> {
                reader.stepIn()
                val elements = mutableListOf<Datum>()
                reader.loadEachValue { elements.add(readJson(reader, reader.typeAnnotations.toList())) }
                reader.stepOut()
                Datum.jsonArray(elements)
            }
            IonType.STRUCT -> {
                reader.stepIn()
                val members = mutableListOf<Field>()
                reader.loadEachValue { members.add(Field.of(reader.fieldName, readJson(reader, reader.typeAnnotations.toList()))) }
                reader.stepOut()
                Datum.jsonObject(members)
            }
            IonType.NULL, IonType.SYMBOL, IonType.TIMESTAMP, IonType.CLOB, IonType.BLOB, IonType.SEXP, IonType.DATAGRAM ->
                throw invalidJson("an Ion ${type.name.lowercase()} is not allowed")
        }
    }

    /**
     * A JSON number written as an Ion decimal. The limits are this runner's, not the schema doc's: a negative
     * scale, as in 1d3, and more than 38 digits, PartiQL's largest DECIMAL precision, are rejected. Both are checked
     * before the value is built, so a value such as 1d50000000 never expands.
     */
    private fun readJsonDecimal(reader: IonReader): Datum {
        val d = reader.bigDecimalValue()
        if (d.scale() < 0) {
            throw invalidJson("this runner does not read a decimal with a negative scale, such as 1d3 (write 1000.)")
        }
        if (d.precision() > JSON_MAX_DECIMAL_DIGITS || d.scale() > JSON_MAX_DECIMAL_DIGITS) {
            throw invalidJson("this runner reads a decimal of at most $JSON_MAX_DECIMAL_DIGITS digits")
        }
        return Datum.decimal(d, maxOf(d.precision(), d.scale()), d.scale())
    }

    /**
     * A datetime scalar inside $json, read strictly: every field the schema doc lists, with no annotations and no
     * other fields. An offset of null gives a TIME or TIMESTAMP without a time zone, so it never matches an
     * offset of 0. Plain $time and $timestamp outside $json still read a null offset as UTC, a known deviation
     * from the doc that is kept so existing results don't move.
     */
    private fun readJsonDatetime(reader: IonReader, annotation: String): Datum {
        val names = when (annotation) {
            PARTIQL_ANNOTATION.DATE_ANNOTATION.annotation -> listOf("year", "month", "day")
            PARTIQL_ANNOTATION.TIME_ANNOTATION.annotation -> listOf("hour", "minute", "second", "offset")
            else -> listOf("year", "month", "day", "hour", "minute", "second", "offset")
        }
        val ints = mutableMapOf<String, Int>()
        var second: BigDecimal? = null
        var offset: Int? = null
        val seen = mutableSetOf<String>()
        reader.stepIn()
        while (reader.next() != null) {
            val name = reader.fieldName
            when {
                name !in names -> throw invalidJson("$annotation has an unexpected field $name")
                !seen.add(name) -> throw invalidJson("$annotation repeats the field $name")
                reader.typeAnnotations.isNotEmpty() -> throw invalidJson("the field $name of $annotation can't have annotations")
            }
            val type = reader.type!!
            when (name) {
                "second" -> {
                    if (type != IonType.DECIMAL || reader.isNullValue) {
                        throw invalidJson("the field second of $annotation must be an Ion decimal, such as 59. or 59.5")
                    }
                    val s = reader.bigDecimalValue()
                    if (s.scale() > TIME_MAX_PRECISION || s.signum() < 0 || s >= BigDecimal(SecondPerMinute)) {
                        throw invalidJson("this runner reads the field second of $annotation from 0 to less than $SecondPerMinute, with at most $TIME_MAX_PRECISION digits after the point")
                    }
                    second = s
                }
                "offset" -> offset = when {
                    type == IonType.NULL && reader.isNullValue -> null
                    type == IonType.INT && !reader.isNullValue -> {
                        val minutes = jsonDatetimeInt(reader, annotation, name)
                        if (minutes !in -JSON_MAX_OFFSET_MINUTES..JSON_MAX_OFFSET_MINUTES) {
                            throw invalidJson("this runner reads the field offset of $annotation from -$JSON_MAX_OFFSET_MINUTES to $JSON_MAX_OFFSET_MINUTES minutes")
                        }
                        minutes
                    }
                    else -> throw invalidJson("the field offset of $annotation must be an Ion int or null, in minutes from UTC")
                }
                else -> {
                    if (type != IonType.INT || reader.isNullValue) {
                        throw invalidJson("the field $name of $annotation must be an Ion int")
                    }
                    ints[name] = jsonDatetimeInt(reader, annotation, name)
                }
            }
        }
        reader.stepOut()
        names.firstOrNull { it !in seen }?.let { throw invalidJson("$annotation has no field $it") }
        ints["year"]?.let { year ->
            if (year !in 1..9999) {
                throw invalidJson("the field year of $annotation must be from 1 to 9999 (SQL:2023 Part 2, Table 13)")
            }
        }
        try {
            val date = if ("year" in ints) LocalDate.of(ints["year"]!!, ints["month"]!!, ints["day"]!!) else null
            val zone = offset?.let { ZoneOffset.ofTotalSeconds(it * SecondPerMinute) }
            val s = second ?: return Datum.date(date!!)
            val time = LocalTime.of(ints["hour"]!!, ints["minute"]!!, s.toInt(), s.remainder(BigDecimal.ONE).movePointRight(TIME_MAX_PRECISION).toInt())
            return when {
                date == null && zone == null -> Datum.time(time, TIME_MAX_PRECISION)
                date == null -> Datum.timez(OffsetTime.of(time, zone), TIME_MAX_PRECISION)
                zone == null -> Datum.timestamp(LocalDateTime.of(date, time), TIME_MAX_PRECISION)
                else -> Datum.timestampz(OffsetDateTime.of(date, time, zone), TIME_MAX_PRECISION)
            }
        } catch (e: DateTimeException) {
            throw invalidJson("$annotation is not a valid ${annotation.removePrefix("$")}: ${e.message}", e)
        }
    }

    private fun jsonDatetimeInt(reader: IonReader, annotation: String, name: String): Int = try {
        reader.bigIntegerValue().intValueExact()
    } catch (e: ArithmeticException) {
        throw invalidJson("the field $name of $annotation is out of range", e)
    }

    private fun invalidJson(message: String, cause: Throwable? = null) = IllegalArgumentException("Invalid $JSON_ANNOTATION value: $message", cause)

    private val JSON_DATETIME_ANNOTATIONS = setOf(
        PARTIQL_ANNOTATION.DATE_ANNOTATION.annotation,
        PARTIQL_ANNOTATION.TIME_ANNOTATION.annotation,
        PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION.annotation,
    )

    private fun <T> IonReader.loadEachValue(block: () -> T) {
        while (this.next() != null) {
            block()
        }
    }

    private fun getPartiQLReservedAnnotation(partiqlAnnotation: List<String>): PARTIQL_ANNOTATION? {
        // $map::keytype::valuetype — $map is 3rd from last when annotations.size >= 3
        if (partiqlAnnotation.size >= 3 && partiqlAnnotation[partiqlAnnotation.size - 3] == PARTIQL_ANNOTATION.MAP_ANNOTATION.annotation) {
            return PARTIQL_ANNOTATION.MAP_ANNOTATION
        }
        // For all other PartiQL annotations, the last annotation is the PartiQL one
        return partiqlAnnotation.lastOrNull()?.let { lastAnnotation ->
            PARTIQL_ANNOTATION.values().find { it.annotation == lastAnnotation }
        }
    }

    /**
     * Converts a PType name string (e.g., "string", "integer", "decimal") to a PType.
     */
    private fun pTypeFromName(name: String): PType = when (name.lowercase()) {
        "dynamic" -> PType.dynamic()
        "bool" -> PType.bool()
        "tinyint" -> PType.tinyint()
        "smallint" -> PType.smallint()
        "integer", "int" -> PType.integer()
        "bigint" -> PType.bigint()
        "numeric" -> PType.numeric()
        "decimal" -> PType.decimal(38, 19)
        "real" -> PType.real()
        "double" -> PType.doublePrecision()
        "string" -> PType.string()
        "char" -> PType.character(255)
        "varchar" -> PType.varchar(255)
        "date" -> PType.date()
        "time" -> PType.time(6)
        "timestamp" -> PType.timestamp(6)
        else -> error("unsupported PType name: $name")
    }

    /**
     * Extracts key and value PTypes from map annotations.
     * The last 3 annotations are: $map::keytype::valuetype
     * e.g., user_ann::$map::string::integer::[...]
     * TODO: support $map::[...] without type annotations when dynamic keys are allowed
     */
    private fun getMapTypes(annotations: List<String>): Pair<PType, PType> {
        if (annotations.size >= 3 && annotations[annotations.size - 3] == "\$map") {
            val keyType = pTypeFromName(annotations[annotations.size - 2])
            val valueType = pTypeFromName(annotations[annotations.size - 1])
            return keyType to valueType
        }
        error("MAP annotation requires key and value types: \$map::keytype::valuetype::[...]. Got annotations: $annotations")
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
