package org.partiql.spi.internal.value.ion

import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.ElementType.BLOB
import com.amazon.ionelement.api.ElementType.BOOL
import com.amazon.ionelement.api.ElementType.CLOB
import com.amazon.ionelement.api.ElementType.DECIMAL
import com.amazon.ionelement.api.ElementType.FLOAT
import com.amazon.ionelement.api.ElementType.INT
import com.amazon.ionelement.api.ElementType.LIST
import com.amazon.ionelement.api.ElementType.SEXP
import com.amazon.ionelement.api.ElementType.STRING
import com.amazon.ionelement.api.ElementType.STRUCT
import com.amazon.ionelement.api.ElementType.SYMBOL
import com.amazon.ionelement.api.ElementType.TIMESTAMP
import com.amazon.ionelement.api.IntElementSize
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

/**
 * A [Datum] implemented over Ion's [AnyElement].
 */
internal class IonVariant(private var value: AnyElement) : Datum {

    /**
     * VARIANT<ION>
     */
    private var type = PType.variant("ion")

    /**
     * Pack an IonDatum into a UTF-8 string byte[] using the textual Ion encoding.
     *
     * @param charset
     * @return
     */
    override fun pack(charset: Charset?): ByteArray {
        val buffer = ByteArrayOutputStream()
        val writer = when (charset) {
            null -> IonBinaryWriterBuilder.standard().build(buffer)
            StandardCharsets.UTF_8, StandardCharsets.US_ASCII -> IonTextWriterBuilder.standard().build(buffer)
            else -> return super.pack(charset) // unsupported
        }
        value.writeTo(writer)
        return buffer.toByteArray()
    }

    override fun lower(): Datum {
        if (value.isNull) {
            return Datum.nullValue(value.type.toPType())
        }
        return when (value.type) {
            STRING -> Datum.string(value.stringValue)
            SYMBOL -> Datum.string(value.symbolValue)
            BOOL -> Datum.bool(value.booleanValue)
            CLOB -> Datum.clob(value.clobValue.copyOfBytes())
            BLOB -> Datum.blob(value.blobValue.copyOfBytes())
            TIMESTAMP -> {
                val ts = value.timestampValue
                val ot = ts.toOffsetDateTime()
                Datum.timestampz(ot, 9)
            }
            INT -> {
                when (value.integerSize) {
                    IntElementSize.LONG -> {
                        val long = value.longValue
                        when (long < Int.MAX_VALUE && long > Int.MIN_VALUE) {
                            true -> Datum.integer(long.toInt())
                            false -> Datum.bigint(long)
                        }
                    }
                    IntElementSize.BIG_INTEGER -> {
                        val dec = value.bigIntegerValue.toBigDecimal()
                        Datum.decimal(dec, dec.precision(), 0)
                    }
                }
            }
            FLOAT -> Datum.doublePrecision(value.doubleValue)
            DECIMAL -> {
                val decimal = value.decimalValue.bigDecimalValue()
                Datum.decimal(decimal, decimal.precision(), decimal.scale())
            }
            LIST -> Datum.array(value.listValues.map { IonVariant(it) })
            SEXP -> Datum.array(value.sexpValues.map { IonVariant(it) })
            STRUCT -> Datum.struct(value.structFields.map { Field.of(it.name, IonVariant(it.value)) })
            ElementType.NULL -> error("The NULL type is impossible to be received.")
        }
    }

    /**
     * This returns the [PType] of a null Ion value, given its [ElementType].
     */
    private fun ElementType.toPType(): PType {
        val code = when (this) {
            SYMBOL, STRING -> PType.STRING
            BOOL -> PType.BOOL
            CLOB -> PType.CLOB
            BLOB -> PType.BLOB
            TIMESTAMP -> PType.TIMESTAMP
            INT -> PType.INTEGER
            FLOAT -> PType.DOUBLE
            DECIMAL -> PType.DECIMAL
            LIST, SEXP -> PType.ARRAY
            STRUCT -> PType.STRUCT
            ElementType.NULL -> PType.UNKNOWN
        }
        return PType.of(code)
    }

    override fun getType(): PType = type

    override fun isNull(): Boolean = false

    override fun isMissing(): Boolean = false

    override fun getString(): String = when (value.type) {
        SYMBOL -> value.stringValue
        STRING -> value.stringValue
        else -> super.getString()
    }

    override fun getBoolean(): Boolean = when (value.type) {
        BOOL -> value.booleanValue
        else -> super.getBoolean()
    }

    override fun getBytes(): ByteArray = when (value.type) {
        CLOB -> value.clobValue.copyOfBytes()
        BLOB -> value.blobValue.copyOfBytes()
        else -> super.getBytes()
    }

    /**
     * Ion timestamp with "whole-day precision" and no time component.
     */
    override fun getLocalDate(): LocalDate {
        if (value.type != TIMESTAMP) {
            return super.getLocalDate()
        }
        val ts = value.timestampValue
        return LocalDate.of(ts.year, ts.month, ts.day)
    }

    /**
     * Ion does not have a TIME type, only TIMESTAMP, so use lower() then coerce.
     */
    override fun getLocalTime(): LocalTime {
        throw IllegalArgumentException("getLocalTime() not supported, use lower() or getLocalDateTime()")
    }

    /**
     * Ion does not have TIMEZ type, only TIMESTAMP, so use lower() then coerce.
     */
    override fun getOffsetTime(): OffsetTime {
        throw IllegalArgumentException("getOffsetTime() not supported, use lower() or getOffsetDateTime()")
    }

    /**
     * Get the OffsetDateTime and return the local part.
     *
     * See: https://github.com/partiql/partiql-lang-kotlin/issues/1689
     */
    override fun getLocalDateTime(): LocalDateTime {
        return offsetDateTime.toLocalDateTime()
    }

    /**
     * Get the OffsetDateTime, using UTC if no offset is given.
     */
    override fun getOffsetDateTime(): OffsetDateTime {
        if (value.type != TIMESTAMP) {
            return super.getOffsetDateTime()
        }
        return value.timestampValue.toOffsetDateTime()
    }

    override fun getBigDecimal(): BigDecimal = when (value.type) {
        INT -> value.bigIntegerValue.toBigDecimal()
        DECIMAL -> value.decimalValue.bigDecimalValue()
        else -> super.getBigDecimal()
    }

    override fun getDouble(): Double = when (value.type) {
        FLOAT -> value.doubleValue
        else -> super.getDouble()
    }

    override fun iterator(): MutableIterator<Datum> = when (value.type) {
        LIST -> value.listValues.map { IonVariant(it) }.toMutableList().iterator()
        SEXP -> value.sexpValues.map { IonVariant(it) }.toMutableList().iterator()
        else -> super.iterator()
    }

    override fun getFields(): MutableIterator<Field> {
        if (value.type != STRUCT) {
            return super.getFields()
        }
        return value.structFields.map { Field.of(it.name, IonVariant(it.value)) }.toMutableList().iterator()
    }

    override fun get(name: String): Datum {
        if (value.type != STRUCT) {
            return super.get(name)
        }
        // TODO handle multiple/ambiguous field names?
        val v = value.asStruct().getOptional(name)
        return if (v == null) {
            Datum.missing()
        } else {
            IonVariant(v)
        }
    }

    override fun getInsensitive(name: String): Datum {
        if (value.type != STRUCT) {
            return super.get(name)
        }
        // TODO handle multiple/ambiguous field names?
        val struct = value.asStruct()
        for (field in struct.fields) {
            if (field.name.equals(name, ignoreCase = true)) {
                return IonVariant(field.value)
            }
        }
        return Datum.missing()
    }

    /**
     * Get the OffsetDateTime from an Ion Timestamp, using UTC if no offset is given.
     */
    private fun Timestamp.toOffsetDateTime(): OffsetDateTime {
        val ts = this
        val tz = when (val o = ts.localOffset) {
            null -> ZoneOffset.UTC
            else -> ZoneOffset.ofHoursMinutes(o / 60, o % 60)
        }
        // [0-59].000_000_000
        val ds = ts.decimalSecond
        val second: Int = ds.toInt()
        val nanoOfSecond: Int = ds.remainder(BigDecimal.ONE).movePointRight(9).toInt()
        // date/time pair
        val date = LocalDate.of(ts.year, ts.month, ts.day)
        val time = LocalTime.of(ts.hour, ts.minute, second, nanoOfSecond)
        return OffsetDateTime.of(date, time, tz)
    }
}
