package org.partiql.beam.io

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.beam.io.runtime.RidlWriter
import java.io.OutputStream

public abstract class BeamWriter(writer: IonWriter) : RidlWriter(writer) {

    public fun write(value: Beam.`$Serializable`): Unit = value.write(this)

    public abstract fun writeShape(value: Beam.Shape)

    public abstract fun writeShapeTBool(value: Beam.Shape.TBool)

    public abstract fun writeShapeTInt8(value: Beam.Shape.TInt8)

    public abstract fun writeShapeTInt16(value: Beam.Shape.TInt16)

    public abstract fun writeShapeTInt32(value: Beam.Shape.TInt32)

    public abstract fun writeShapeTInt64(value: Beam.Shape.TInt64)

    public abstract fun writeShapeTInteger(value: Beam.Shape.TInteger)

    public abstract fun writeShapeTDecimal(value: Beam.Shape.TDecimal)

    public abstract fun writeShapeTNumeric(value: Beam.Shape.TNumeric)

    public abstract fun writeShapeTFloat32(value: Beam.Shape.TFloat32)

    public abstract fun writeShapeTFloat64(value: Beam.Shape.TFloat64)

    public abstract fun writeShapeTFloat(value: Beam.Shape.TFloat)

    public abstract fun writeShapeTCharFixed(value: Beam.Shape.TCharFixed)

    public abstract fun writeShapeTCharVarying(value: Beam.Shape.TCharVarying)

    public abstract fun writeShapeTString(value: Beam.Shape.TString)

    public abstract fun writeShapeTSymbol(value: Beam.Shape.TSymbol)

    public abstract fun writeShapeTClob(value: Beam.Shape.TClob)

    public abstract fun writeShapeTBitFixed(value: Beam.Shape.TBitFixed)

    public abstract fun writeShapeTBitVarying(value: Beam.Shape.TBitVarying)

    public abstract fun writeShapeTBinary(value: Beam.Shape.TBinary)

    public abstract fun writeShapeTByteFixed(value: Beam.Shape.TByteFixed)

    public abstract fun writeShapeTByteVarying(value: Beam.Shape.TByteVarying)

    public abstract fun writeShapeTBlob(value: Beam.Shape.TBlob)

    public abstract fun writeShapeTDate(value: Beam.Shape.TDate)

    public abstract fun writeShapeTTime(value: Beam.Shape.TTime)

    public abstract fun writeShapeTTimeTz(value: Beam.Shape.TTimeTz)

    public abstract fun writeShapeTTimestamp(value: Beam.Shape.TTimestamp)

    public abstract fun writeShapeTTimestampTz(value: Beam.Shape.TTimestampTz)

    public abstract fun writeShapeTBag(value: Beam.Shape.TBag)

    public abstract fun writeShapeTList(value: Beam.Shape.TList)

    public abstract fun writeShapeTSexp(value: Beam.Shape.TSexp)

    public abstract fun writeShapeTStruct(value: Beam.Shape.TStruct)

    public abstract fun writeShapeTNull(value: Beam.Shape.TNull)

    public abstract fun writeShapeTMissing(value: Beam.Shape.TMissing)

    public abstract fun writeShapeTUnion(value: Beam.Shape.TUnion)

    public abstract fun writeShapeTDynamic(value: Beam.Shape.TDynamic)

    public abstract fun writeShapes(value: Beam.Shapes)

    public abstract fun writeFields(value: Beam.Fields)

    public abstract fun writeField(value: Beam.Field)

    public companion object {

        @JvmStatic
        public fun text(out: Appendable): BeamWriter = Text(IonTextWriterBuilder.standard().build(out))

        @JvmStatic
        public fun text(writer: IonWriter): BeamWriter = Text(writer)

        @JvmStatic
        public fun packed(out: OutputStream): BeamWriter = Packed(IonBinaryWriterBuilder.standard().build(out))

        @JvmStatic
        public fun packed(writer: IonWriter): BeamWriter = Packed(writer)
    }

    private class Text(private val writer: IonWriter) : BeamWriter(writer) {

        override fun writeShape(value: Beam.Shape) {
            when (value) {
                is Beam.Shape.TBool -> writeShapeTBool(value)
                is Beam.Shape.TInt8 -> writeShapeTInt8(value)
                is Beam.Shape.TInt16 -> writeShapeTInt16(value)
                is Beam.Shape.TInt32 -> writeShapeTInt32(value)
                is Beam.Shape.TInt64 -> writeShapeTInt64(value)
                is Beam.Shape.TInteger -> writeShapeTInteger(value)
                is Beam.Shape.TDecimal -> writeShapeTDecimal(value)
                is Beam.Shape.TNumeric -> writeShapeTNumeric(value)
                is Beam.Shape.TFloat32 -> writeShapeTFloat32(value)
                is Beam.Shape.TFloat64 -> writeShapeTFloat64(value)
                is Beam.Shape.TFloat -> writeShapeTFloat(value)
                is Beam.Shape.TCharFixed -> writeShapeTCharFixed(value)
                is Beam.Shape.TCharVarying -> writeShapeTCharVarying(value)
                is Beam.Shape.TString -> writeShapeTString(value)
                is Beam.Shape.TSymbol -> writeShapeTSymbol(value)
                is Beam.Shape.TClob -> writeShapeTClob(value)
                is Beam.Shape.TBitFixed -> writeShapeTBitFixed(value)
                is Beam.Shape.TBitVarying -> writeShapeTBitVarying(value)
                is Beam.Shape.TBinary -> writeShapeTBinary(value)
                is Beam.Shape.TByteFixed -> writeShapeTByteFixed(value)
                is Beam.Shape.TByteVarying -> writeShapeTByteVarying(value)
                is Beam.Shape.TBlob -> writeShapeTBlob(value)
                is Beam.Shape.TDate -> writeShapeTDate(value)
                is Beam.Shape.TTime -> writeShapeTTime(value)
                is Beam.Shape.TTimeTz -> writeShapeTTimeTz(value)
                is Beam.Shape.TTimestamp -> writeShapeTTimestamp(value)
                is Beam.Shape.TTimestampTz -> writeShapeTTimestampTz(value)
                is Beam.Shape.TBag -> writeShapeTBag(value)
                is Beam.Shape.TList -> writeShapeTList(value)
                is Beam.Shape.TSexp -> writeShapeTSexp(value)
                is Beam.Shape.TStruct -> writeShapeTStruct(value)
                is Beam.Shape.TNull -> writeShapeTNull(value)
                is Beam.Shape.TMissing -> writeShapeTMissing(value)
                is Beam.Shape.TUnion -> writeShapeTUnion(value)
                is Beam.Shape.TDynamic -> writeShapeTDynamic(value)
            }
        }

        override fun writeShapeTBool(value: Beam.Shape.TBool) {
            writer.setTypeAnnotations("shape.t_bool")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt8(value: Beam.Shape.TInt8) {
            writer.setTypeAnnotations("shape.t_int8")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt16(value: Beam.Shape.TInt16) {
            writer.setTypeAnnotations("shape.t_int16")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt32(value: Beam.Shape.TInt32) {
            writer.setTypeAnnotations("shape.t_int32")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt64(value: Beam.Shape.TInt64) {
            writer.setTypeAnnotations("shape.t_int64")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInteger(value: Beam.Shape.TInteger) {
            writer.setTypeAnnotations("shape.t_integer")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTDecimal(value: Beam.Shape.TDecimal) {
            writer.setTypeAnnotations("shape.t_decimal")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTNumeric(value: Beam.Shape.TNumeric) {
            writer.setTypeAnnotations("shape.t_numeric")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.setFieldName("scale"); writer.writeInt(value.scale)
            writer.stepOut()
        }

        override fun writeShapeTFloat32(value: Beam.Shape.TFloat32) {
            writer.setTypeAnnotations("shape.t_float32")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTFloat64(value: Beam.Shape.TFloat64) {
            writer.setTypeAnnotations("shape.t_float64")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTFloat(value: Beam.Shape.TFloat) {
            writer.setTypeAnnotations("shape.t_float")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTCharFixed(value: Beam.Shape.TCharFixed) {
            writer.setTypeAnnotations("shape.t_char_fixed")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTCharVarying(value: Beam.Shape.TCharVarying) {
            writer.setTypeAnnotations("shape.t_char_varying")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTString(value: Beam.Shape.TString) {
            writer.setTypeAnnotations("shape.t_string")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTSymbol(value: Beam.Shape.TSymbol) {
            writer.setTypeAnnotations("shape.t_symbol")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTClob(value: Beam.Shape.TClob) {
            writer.setTypeAnnotations("shape.t_clob")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTBitFixed(value: Beam.Shape.TBitFixed) {
            writer.setTypeAnnotations("shape.t_bit_fixed")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBitVarying(value: Beam.Shape.TBitVarying) {
            writer.setTypeAnnotations("shape.t_bit_varying")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBinary(value: Beam.Shape.TBinary) {
            writer.setTypeAnnotations("shape.t_binary")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTByteFixed(value: Beam.Shape.TByteFixed) {
            writer.setTypeAnnotations("shape.t_byte_fixed")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTByteVarying(value: Beam.Shape.TByteVarying) {
            writer.setTypeAnnotations("shape.t_byte_varying")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("length"); writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBlob(value: Beam.Shape.TBlob) {
            writer.setTypeAnnotations("shape.t_blob")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTDate(value: Beam.Shape.TDate) {
            writer.setTypeAnnotations("shape.t_date")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTTime(value: Beam.Shape.TTime) {
            writer.setTypeAnnotations("shape.t_time")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTTimeTz(value: Beam.Shape.TTimeTz) {
            writer.setTypeAnnotations("shape.t_time_tz")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.setFieldName("offset_hour"); writer.writeInt(value.offsetHour)
            writer.setFieldName("offset_minute"); writer.writeInt(value.offsetMinute)
            writer.stepOut()
        }

        override fun writeShapeTTimestamp(value: Beam.Shape.TTimestamp) {
            writer.setTypeAnnotations("shape.t_timestamp")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTTimestampTz(value: Beam.Shape.TTimestampTz) {
            writer.setTypeAnnotations("shape.t_timestamp_tz")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("precision"); writer.writeInt(value.precision)
            writer.setFieldName("offset_hour"); writer.writeInt(value.offsetHour)
            writer.setFieldName("offset_minute"); writer.writeInt(value.offsetMinute)
            writer.stepOut()
        }

        override fun writeShapeTBag(value: Beam.Shape.TBag) {
            writer.setTypeAnnotations("shape.t_bag")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("items"); write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTList(value: Beam.Shape.TList) {
            writer.setTypeAnnotations("shape.t_list")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("items"); write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTSexp(value: Beam.Shape.TSexp) {
            writer.setTypeAnnotations("shape.t_sexp")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("items"); write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTStruct(value: Beam.Shape.TStruct) {
            writer.setTypeAnnotations("shape.t_struct")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("fields"); write(value.fields)
            writer.setFieldName("is_closed"); writer.writeBool(value.isClosed)
            writer.setFieldName("is_ordered"); writer.writeBool(value.isOrdered)
            writer.setFieldName("has_unique_fields"); writer.writeBool(value.hasUniqueFields)
            writer.stepOut()
        }

        override fun writeShapeTNull(value: Beam.Shape.TNull) {
            writer.setTypeAnnotations("shape.t_null")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTMissing(value: Beam.Shape.TMissing) {
            writer.setTypeAnnotations("shape.t_missing")
            writer.writeSymbol("unit")
        }

        override fun writeShapeTUnion(value: Beam.Shape.TUnion) {
            writer.setTypeAnnotations("shape.t_union")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("shapes"); write(value.shapes)
            writer.stepOut()
        }

        override fun writeShapeTDynamic(value: Beam.Shape.TDynamic) {
            writer.setTypeAnnotations("shape.t_dynamic")
            writer.writeSymbol("unit")
        }

        override fun writeShapes(value: Beam.Shapes) {
            writer.setTypeAnnotations("shapes")
            writer.stepIn(IonType.LIST)
            for (item in value) {
                write(item)
            }
            writer.stepOut()
        }

        override fun writeFields(value: Beam.Fields) {
            writer.setTypeAnnotations("fields")
            writer.stepIn(IonType.LIST)
            for (item in value) {
                write(item)
            }
            writer.stepOut()
        }

        override fun writeField(value: Beam.Field) {
            writer.setTypeAnnotations("field")
            writer.stepIn(IonType.STRUCT)
            writer.setFieldName("name"); writer.writeString(value.name)
            writer.setFieldName("shape"); write(value.shape)
            writer.stepOut()
        }
    }

    private class Packed(private val writer: IonWriter) : BeamWriter(writer) {

        override fun writeShape(value: Beam.Shape) {
        }

        override fun writeShapeTBool(value: Beam.Shape.TBool) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt8(value: Beam.Shape.TInt8) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt16(value: Beam.Shape.TInt16) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt32(value: Beam.Shape.TInt32) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInt64(value: Beam.Shape.TInt64) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTInteger(value: Beam.Shape.TInteger) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTDecimal(value: Beam.Shape.TDecimal) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTNumeric(value: Beam.Shape.TNumeric) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.writeInt(value.scale)
            writer.stepOut()
        }

        override fun writeShapeTFloat32(value: Beam.Shape.TFloat32) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTFloat64(value: Beam.Shape.TFloat64) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTFloat(value: Beam.Shape.TFloat) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTCharFixed(value: Beam.Shape.TCharFixed) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTCharVarying(value: Beam.Shape.TCharVarying) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTString(value: Beam.Shape.TString) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTSymbol(value: Beam.Shape.TSymbol) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTClob(value: Beam.Shape.TClob) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTBitFixed(value: Beam.Shape.TBitFixed) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBitVarying(value: Beam.Shape.TBitVarying) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBinary(value: Beam.Shape.TBinary) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTByteFixed(value: Beam.Shape.TByteFixed) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTByteVarying(value: Beam.Shape.TByteVarying) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.length)
            writer.stepOut()
        }

        override fun writeShapeTBlob(value: Beam.Shape.TBlob) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTDate(value: Beam.Shape.TDate) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTTime(value: Beam.Shape.TTime) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTTimeTz(value: Beam.Shape.TTimeTz) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.writeInt(value.offsetHour)
            writer.writeInt(value.offsetMinute)
            writer.stepOut()
        }

        override fun writeShapeTTimestamp(value: Beam.Shape.TTimestamp) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.stepOut()
        }

        override fun writeShapeTTimestampTz(value: Beam.Shape.TTimestampTz) {
            writer.stepIn(IonType.SEXP)
            writer.writeInt(value.precision)
            writer.writeInt(value.offsetHour)
            writer.writeInt(value.offsetMinute)
            writer.stepOut()
        }

        override fun writeShapeTBag(value: Beam.Shape.TBag) {
            writer.stepIn(IonType.SEXP)
            write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTList(value: Beam.Shape.TList) {
            writer.stepIn(IonType.SEXP)
            write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTSexp(value: Beam.Shape.TSexp) {
            writer.stepIn(IonType.SEXP)
            write(value.items)
            writer.stepOut()
        }

        override fun writeShapeTStruct(value: Beam.Shape.TStruct) {
            writer.stepIn(IonType.SEXP)
            write(value.fields)
            writer.writeBool(value.isClosed)
            writer.writeBool(value.isOrdered)
            writer.writeBool(value.hasUniqueFields)
            writer.stepOut()
        }

        override fun writeShapeTNull(value: Beam.Shape.TNull) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTMissing(value: Beam.Shape.TMissing) {
            writer.writeSymbol("unit")
        }

        override fun writeShapeTUnion(value: Beam.Shape.TUnion) {
            writer.stepIn(IonType.SEXP)
            write(value.shapes)
            writer.stepOut()
        }

        override fun writeShapeTDynamic(value: Beam.Shape.TDynamic) {
            writer.writeSymbol("unit")
        }

        override fun writeShapes(value: Beam.Shapes) {
            writer.stepIn(IonType.LIST)
            for (item in value) {
                write(item)
            }
            writer.stepOut()
        }

        override fun writeFields(value: Beam.Fields) {
            writer.stepIn(IonType.LIST)
            for (item in value) {
                write(item)
            }
            writer.stepOut()
        }

        override fun writeField(value: Beam.Field) {
            writer.stepIn(IonType.SEXP)
            writer.writeString(value.name)
            write(value.shape)
            writer.stepOut()
        }
    }
}
