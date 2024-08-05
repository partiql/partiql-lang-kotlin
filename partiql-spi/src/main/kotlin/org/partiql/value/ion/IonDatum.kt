package org.partiql.value.ion

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
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Field
import org.partiql.planner.internal.SqlTypes
import org.partiql.types.PType
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.math.BigInteger

/**
 * A [Datum] implemented over Ion's [AnyElement].
 */
public class IonDatum private constructor(value: AnyElement) : Datum {

    // DO NOT USE FINAL
    private var _value = value
    private var _kind = value.type
    private var _type = when (value.type) {
        NULL -> SqlTypes.dynamic()
        BOOL -> SqlTypes.bool()
        INT -> SqlTypes.numeric()
        FLOAT -> SqlTypes.double()
        DECIMAL -> SqlTypes.decimal()
        STRING -> SqlTypes.string()
        CLOB -> SqlTypes.clob()
        BLOB -> SqlTypes.blob()
        LIST -> SqlTypes.array()
        STRUCT -> SqlTypes.struct()
        // ???
        SEXP -> PType.typeSexp()
        SYMBOL -> PType.typeSymbol()
        TIMESTAMP -> error("PType has no arbitrary timestamp")
    }

    public companion object {

        /**
         * TODO reader/writer ?? or check annotations
         *
         * @param value
         * @return
         */
        @JvmStatic
        public fun of(value: AnyElement): IonDatum {
            return IonDatum(value)
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
        TODO("IonDatum does not support DATE")
    }

    override fun getTime(): Time {
        TODO("IonDatum does not support TIME")
    }

    override fun getTimestamp(): Timestamp {
        TODO("IonDatum does not support TIMESTAMP")
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
        // TODO handle nulls?
        return if (v == null) {
            Datum.nullValue()
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
        // TODO handle missing fields?
        return Datum.nullValue()
    }
}
