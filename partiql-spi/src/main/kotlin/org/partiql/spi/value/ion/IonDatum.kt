package org.partiql.spi.value.ion

import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType.BOOL
import com.amazon.ionelement.api.ElementType.DECIMAL
import com.amazon.ionelement.api.ElementType.FLOAT
import com.amazon.ionelement.api.ElementType.INT
import com.amazon.ionelement.api.ElementType.LIST
import com.amazon.ionelement.api.ElementType.SEXP
import com.amazon.ionelement.api.ElementType.STRING
import com.amazon.ionelement.api.ElementType.STRUCT
import com.amazon.ionelement.api.ElementType.SYMBOL
import com.amazon.ionelement.api.ElementType.TIMESTAMP
import com.amazon.ionelement.api.IonElement
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.spi.value.Variant
import org.partiql.types.PType
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * A [Datum] implemented over Ion's [AnyElement].
 */
public class IonDatum(private var value: AnyElement) : Variant<IonElement> {

    /**
     * TODO replace with PType.variant("ion")
     */
    private var type = PType.unknown()

    /**
     * Unpack the inner Ion value.
     *
     * @return IonElement
     */
    override fun unpack(): IonElement = value

    /**
     * Pack an IonDatum into byte[] using the binary Ion encoding.
     *
     * @return byte[]
     */
    override fun pack(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val writer = IonBinaryWriterBuilder.standard().build(buffer)
        value.writeTo(writer)
        return buffer.toByteArray()
    }

    /**
     * Pack an IonDatum into a UTF-8 string byte[] using the textual Ion encoding.
     *
     * @param charset
     * @return
     */
    override fun pack(charset: Charset): ByteArray {
        if (charset != StandardCharsets.UTF_8 || charset != StandardCharsets.US_ASCII) {
            // unsupported
            return super.pack(charset)
        }
        val buffer = ByteArrayOutputStream()
        val writer = IonTextWriterBuilder.standard().build(buffer)
        value.writeTo(writer)
        return buffer.toByteArray()
    }

    override fun getType(): PType = type

    override fun isNull(): Boolean = value.isNull

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

    // override fun getBytes(): ByteArray =  when (value.type) {
    //     CLOB -> value.clobValue.copyOfBytes()
    //     BLOB -> value.blobValue.copyOfBytes()
    //     else -> super.getBytes()
    // }
    //
    // override fun getByte(): Byte {
    //     return super.getByte()
    // }

    override fun getDate(): Date {
        return when (value.type) {
            TIMESTAMP -> {
                val ts = value.timestampValue
                DateTimeValue.date(ts.year, ts.month, ts.day)
            }
            else -> super.getDate()
        }
    }

    override fun getTime(): Time {
        return when (value.type) {
            TIMESTAMP -> {
                val ts = value.timestampValue
                val tz = when (ts.localOffset) {
                    null -> TimeZone.UnknownTimeZone
                    else -> TimeZone.UtcOffset.of(ts.zHour, ts.zMinute)
                }
                DateTimeValue.time(ts.hour, ts.minute, ts.second, tz)
            }
            else -> super.getTime()
        }
    }

    // TODO: Handle struct notation
    override fun getTimestamp(): Timestamp {
        return when (value.type) {
            TIMESTAMP -> DateTimeValue.timestamp(value.timestampValue)
            else -> super.getTimestamp()
        }
    }

    override fun getBigInteger(): BigInteger = when (value.type) {
        INT -> value.bigIntegerValue
        else -> super.getBigInteger()
    }

    override fun getDouble(): Double = when (value.type) {
        FLOAT -> value.doubleValue
        else -> super.getDouble()
    }

    override fun getBigDecimal(): BigDecimal = when (value.type) {
        DECIMAL -> value.decimalValue.bigDecimalValue()
        else -> super.getBigDecimal()
    }

    override fun iterator(): MutableIterator<Datum> = when (value.type) {
        LIST -> value.listValues.map { IonDatum(it) }.toMutableList().iterator()
        SEXP -> value.sexpValues.map { IonDatum(it) }.toMutableList().iterator()
        else -> super.iterator()
    }

    override fun getFields(): MutableIterator<Field> {
        if (value.type != STRUCT) {
            return super.getFields()
        }
        return value.structFields
            .map { Field.of(it.name, IonDatum(it.value)) }
            .toMutableList()
            .iterator()
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
            IonDatum(v)
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
                return IonDatum(field.value)
            }
        }
        return Datum.missing()
    }
}
