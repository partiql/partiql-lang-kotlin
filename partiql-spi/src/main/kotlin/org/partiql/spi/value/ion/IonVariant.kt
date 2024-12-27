package org.partiql.spi.value.ion

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
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType
import org.partiql.value.datetime.DateTimeValue
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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
            TIMESTAMP -> Datum.timestamp(DateTimeValue.timestamp(value.timestampValue))
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
}
