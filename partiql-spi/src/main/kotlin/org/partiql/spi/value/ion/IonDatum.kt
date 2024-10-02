package org.partiql.spi.value.ion

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType.BLOB
import com.amazon.ionelement.api.ElementType.BOOL
import com.amazon.ionelement.api.ElementType.CLOB
import com.amazon.ionelement.api.ElementType.DECIMAL
import com.amazon.ionelement.api.ElementType.FLOAT
import com.amazon.ionelement.api.ElementType.INT
import com.amazon.ionelement.api.ElementType.LIST
import com.amazon.ionelement.api.ElementType.NULL
import com.amazon.ionelement.api.ElementType.SEXP
import com.amazon.ionelement.api.ElementType.STRING
import com.amazon.ionelement.api.ElementType.STRUCT
import com.amazon.ionelement.api.ElementType.SYMBOL
import com.amazon.ionelement.api.ElementType.TIMESTAMP
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.math.BigInteger

/**
 * A [Datum] implemented over Ion's [AnyElement].
 */
public class IonDatum private constructor(value: AnyElement, type: PType) :
    Datum {

    // DO NOT USE FINAL
    private var _value = value
    private var _type = type
    private var _kind = value.type

    /**
     * Some encoding of PartiQL values as Ion.
     */
    private enum class Annotation(val symbol: String) {
        MISSING("\$missing"),
        BAG("\$bag"),
        DATE("\$date"),
        TIME("\$time"),
        TIMESTAMP("\$timestamp"),
        GRAPH("\$graph");

        override fun toString(): String = symbol

        companion object {

            @JvmStatic
            fun of(value: AnyElement): Annotation? = value.annotations.lastOrNull()?.let {
                Annotation.values().find { a -> a.symbol == it }
            }
        }
    }

    public companion object {

        /**
         * TODO reader/writer ?? or check annotations
         *
         * @param value
         * @return
         */
        @JvmStatic
        public fun of(value: AnyElement): Datum {
            val tag = Annotation.of(value)
            val type = when (value.type) {
                NULL -> return when (tag) {
                    Annotation.MISSING -> Datum.missing()
                    Annotation.BAG -> Datum.nullValue(PType.bag())
                    Annotation.DATE -> Datum.nullValue(PType.date())
                    Annotation.TIME -> Datum.nullValue(PType.time(6))
                    Annotation.TIMESTAMP -> Datum.nullValue(PType.time(6))
                    Annotation.GRAPH -> error("Datum does not support GRAPH type.")
                    null -> Datum.nullValue()
                }
                BOOL -> when (tag) {
                    null -> PType.bool()
                    else -> error("Unexpected type annotation for Ion BOOL: $tag")
                }
                INT -> when (tag) {
                    null -> PType.numeric()
                    else -> error("Unexpected type annotation for Ion INT: $tag")
                }
                FLOAT -> when (tag) {
                    null -> PType.doublePrecision()
                    else -> error("Unexpected type annotation for Ion FLOAT: $tag")
                }
                DECIMAL -> when (tag) {
                    null -> PType.decimal()
                    else -> error("Unexpected type annotation for Ion DECIMAL: $tag")
                }
                STRING -> when (tag) {
                    null -> PType.string()
                    else -> error("Unexpected type annotation for Ion STRING: $tag")
                }
                CLOB -> when (tag) {
                    null -> PType.clob(Int.MAX_VALUE)
                    else -> error("Unexpected type annotation for Ion CLOB: $tag")
                }
                BLOB -> when (tag) {
                    null -> PType.blob(Int.MAX_VALUE)
                    else -> error("Unexpected type annotation for Ion BLOB: $tag")
                }
                LIST -> when (tag) {
                    Annotation.BAG -> PType.bag()
                    null -> PType.array()
                    else -> error("Unexpected type annotation for Ion LIST: $tag")
                }
                STRUCT -> when (tag) {
                    null -> PType.struct()
                    Annotation.DATE -> TODO("IonDatum for DATE not supported")
                    Annotation.TIME -> TODO("IonDatum for TIME not supported")
                    Annotation.TIMESTAMP -> TODO("IonDatum for TIMESTAMP not supported")
                    else -> error("Unexpected type annotation for Ion STRUCT: $tag")
                }
                SEXP -> when (tag) {
                    null -> PType.sexp()
                    else -> error("Unexpected type annotation for Ion SEXP: $tag")
                }
                SYMBOL -> when (tag) {
                    null -> PType.symbol()
                    else -> error("Unexpected type annotation for Ion SYMBOL: $tag")
                }
                TIMESTAMP -> when (tag) {
                    null -> PType.timestamp(6)
                    else -> error("Unexpected type annotation for Ion TIMESTAMP: $tag")
                }
            }
            return IonDatum(value, type)
        }
    }

    override fun getType(): PType = _type

    override fun isNull(): Boolean = _value.isNull

    override fun isMissing(): Boolean = false

    override fun getString(): String = when (_kind) {
        SYMBOL -> _value.stringValue
        STRING -> _value.stringValue
        else -> super.getString()
    }

    override fun getBoolean(): Boolean = when (_kind) {
        BOOL -> _value.booleanValue
        else -> super.getBoolean()
    }

    // override fun getBytes(): ByteArray =  when (_kind) {
    //     CLOB -> _value.clobValue.copyOfBytes()
    //     BLOB -> _value.blobValue.copyOfBytes()
    //     else -> super.getBytes()
    // }
    //
    // override fun getByte(): Byte {
    //     return super.getByte()
    // }

    override fun getDate(): Date {
        return when (_kind) {
            TIMESTAMP -> {
                val ts = _value.timestampValue
                DateTimeValue.date(ts.year, ts.month, ts.day)
            }
            else -> super.getDate()
        }
    }

    override fun getTime(): Time {
        return when (_kind) {
            TIMESTAMP -> {
                val ts = _value.timestampValue
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
        return when (_kind) {
            TIMESTAMP -> DateTimeValue.timestamp(_value.timestampValue)
            else -> super.getTimestamp()
        }
    }

    override fun getBigInteger(): BigInteger = when (_kind) {
        INT -> _value.bigIntegerValue
        else -> super.getBigInteger()
    }

    override fun getDouble(): Double = when (_kind) {
        FLOAT -> _value.doubleValue
        else -> super.getDouble()
    }

    override fun getBigDecimal(): BigDecimal = when (_kind) {
        DECIMAL -> _value.decimalValue.bigDecimalValue()
        else -> super.getBigDecimal()
    }

    override fun iterator(): MutableIterator<Datum> = when (_kind) {
        LIST -> _value.listValues.map { of(it) }.toMutableList().iterator()
        SEXP -> _value.sexpValues.map { of(it) }.toMutableList().iterator()
        else -> super.iterator()
    }

    override fun getFields(): MutableIterator<Field> {
        if (_kind != STRUCT) {
            return super.getFields()
        }
        return _value.structFields
            .map { Field.of(it.name, of(it.value)) }
            .toMutableList()
            .iterator()
    }

    override fun get(name: String): Datum {
        if (_kind != STRUCT) {
            return super.get(name)
        }
        // TODO handle multiple/ambiguous field names?
        val v = _value.asStruct().getOptional(name)
        return if (v == null) {
            Datum.missing()
        } else {
            of(v)
        }
    }

    override fun getInsensitive(name: String): Datum {
        if (_kind != STRUCT) {
            return super.get(name)
        }
        // TODO handle multiple/ambiguous field names?
        val struct = _value.asStruct()
        for (field in struct.fields) {
            if (field.name.equals(name, ignoreCase = true)) {
                return of(field.value)
            }
        }
        return Datum.missing()
    }
}
