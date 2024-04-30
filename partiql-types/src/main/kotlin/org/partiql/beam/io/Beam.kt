package org.partiql.beam.io

import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.beam.io.runtime.RidlList

public class Beam private constructor() {

    public companion object {

        @JvmStatic
        public fun toString(value: `$Serializable`): String {
            val sb = StringBuilder()
            val out = IonTextWriterBuilder.pretty().build(sb)
            val writer = BeamWriter.text(out)
            value.write(writer)
            return sb.toString()
        }
    }

    public interface `$Serializable` {

        public fun write(writer: BeamWriter)

        public fun read(reader: BeamReader): `$Serializable`
    }

    public sealed interface Shape : `$Serializable` {

        override fun write(writer: BeamWriter): Unit = writer.writeShape(this)

        override fun read(reader: BeamReader): Shape = reader.readShape()

        public object TBool : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBool(this)

            override fun read(reader: BeamReader): TBool = reader.readShapeTBool()

            override fun toString(): String = toString(this)
        }

        public object TInt8 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTInt8(this)

            override fun read(reader: BeamReader): TInt8 = reader.readShapeTInt8()

            override fun toString(): String = toString(this)
        }

        public object TInt16 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTInt16(this)

            override fun read(reader: BeamReader): TInt16 = reader.readShapeTInt16()

            override fun toString(): String = toString(this)
        }

        public object TInt32 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTInt32(this)

            override fun read(reader: BeamReader): TInt32 = reader.readShapeTInt32()

            override fun toString(): String = toString(this)
        }

        public object TInt64 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTInt64(this)

            override fun read(reader: BeamReader): TInt64 = reader.readShapeTInt64()

            override fun toString(): String = toString(this)
        }

        public object TInteger : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTInteger(this)

            override fun read(reader: BeamReader): TInteger = reader.readShapeTInteger()

            override fun toString(): String = toString(this)
        }

        public object TDecimal : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTDecimal(this)

            override fun read(reader: BeamReader): TDecimal = reader.readShapeTDecimal()

            override fun toString(): String = toString(this)
        }

        public data class TNumeric(
            @JvmField var precision: Long,
            @JvmField var scale: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTNumeric(this)

            override fun read(reader: BeamReader): TNumeric = reader.readShapeTNumeric()

            override fun toString(): String = toString(this)
        }

        public object TFloat32 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTFloat32(this)

            override fun read(reader: BeamReader): TFloat32 = reader.readShapeTFloat32()

            override fun toString(): String = toString(this)
        }

        public object TFloat64 : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTFloat64(this)

            override fun read(reader: BeamReader): TFloat64 = reader.readShapeTFloat64()

            override fun toString(): String = toString(this)
        }

        public data class TFloat(
            @JvmField var precision: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTFloat(this)

            override fun read(reader: BeamReader): TFloat = reader.readShapeTFloat()

            override fun toString(): String = toString(this)
        }

        public data class TCharFixed(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTCharFixed(this)

            override fun read(reader: BeamReader): TCharFixed = reader.readShapeTCharFixed()

            override fun toString(): String = toString(this)
        }

        public data class TCharVarying(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTCharVarying(this)

            override fun read(reader: BeamReader): TCharVarying = reader.readShapeTCharVarying()

            override fun toString(): String = toString(this)
        }

        public object TString : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTString(this)

            override fun read(reader: BeamReader): TString = reader.readShapeTString()

            override fun toString(): String = toString(this)
        }

        public object TSymbol : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTSymbol(this)

            override fun read(reader: BeamReader): TSymbol = reader.readShapeTSymbol()

            override fun toString(): String = toString(this)
        }

        public object TClob : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTClob(this)

            override fun read(reader: BeamReader): TClob = reader.readShapeTClob()

            override fun toString(): String = toString(this)
        }

        public data class TBitFixed(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBitFixed(this)

            override fun read(reader: BeamReader): TBitFixed = reader.readShapeTBitFixed()

            override fun toString(): String = toString(this)
        }

        public data class TBitVarying(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBitVarying(this)

            override fun read(reader: BeamReader): TBitVarying = reader.readShapeTBitVarying()

            override fun toString(): String = toString(this)
        }

        public object TBinary : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBinary(this)

            override fun read(reader: BeamReader): TBinary = reader.readShapeTBinary()

            override fun toString(): String = toString(this)
        }

        public data class TByteFixed(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTByteFixed(this)

            override fun read(reader: BeamReader): TByteFixed = reader.readShapeTByteFixed()

            override fun toString(): String = toString(this)
        }

        public data class TByteVarying(
            @JvmField var length: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTByteVarying(this)

            override fun read(reader: BeamReader): TByteVarying = reader.readShapeTByteVarying()

            override fun toString(): String = toString(this)
        }

        public object TBlob : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBlob(this)

            override fun read(reader: BeamReader): TBlob = reader.readShapeTBlob()

            override fun toString(): String = toString(this)
        }

        public object TDate : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTDate(this)

            override fun read(reader: BeamReader): TDate = reader.readShapeTDate()

            override fun toString(): String = toString(this)
        }

        public data class TTime(
            @JvmField var precision: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTTime(this)

            override fun read(reader: BeamReader): TTime = reader.readShapeTTime()

            override fun toString(): String = toString(this)
        }

        public data class TTimeTz(
            @JvmField var precision: Long,
            @JvmField var offsetHour: Long,
            @JvmField var offsetMinute: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTTimeTz(this)

            override fun read(reader: BeamReader): TTimeTz = reader.readShapeTTimeTz()

            override fun toString(): String = toString(this)
        }

        public data class TTimestamp(
            @JvmField var precision: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTTimestamp(this)

            override fun read(reader: BeamReader): TTimestamp = reader.readShapeTTimestamp()

            override fun toString(): String = toString(this)
        }

        public data class TTimestampTz(
            @JvmField var precision: Long,
            @JvmField var offsetHour: Long,
            @JvmField var offsetMinute: Long,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTTimestampTz(this)

            override fun read(reader: BeamReader): TTimestampTz = reader.readShapeTTimestampTz()

            override fun toString(): String = toString(this)
        }

        public data class TBag(
            @JvmField var items: Shape,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTBag(this)

            override fun read(reader: BeamReader): TBag = reader.readShapeTBag()

            override fun toString(): String = toString(this)
        }

        public data class TList(
            @JvmField var items: Shape,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTList(this)

            override fun read(reader: BeamReader): TList = reader.readShapeTList()

            override fun toString(): String = toString(this)
        }

        public data class TSexp(
            @JvmField var items: Shape,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTSexp(this)

            override fun read(reader: BeamReader): TSexp = reader.readShapeTSexp()

            override fun toString(): String = toString(this)
        }

        public data class TStruct(
            @JvmField var fields: Fields,
            @JvmField var isClosed: Boolean,
            @JvmField var isOrdered: Boolean,
            @JvmField var hasUniqueFields: Boolean,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTStruct(this)

            override fun read(reader: BeamReader): TStruct = reader.readShapeTStruct()

            override fun toString(): String = toString(this)
        }

        public object TNull : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTNull(this)

            override fun read(reader: BeamReader): TNull = reader.readShapeTNull()

            override fun toString(): String = toString(this)
        }

        public object TMissing : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTMissing(this)

            override fun read(reader: BeamReader): TMissing = reader.readShapeTMissing()

            override fun toString(): String = toString(this)
        }

        public data class TUnion(
            @JvmField var shapes: Shapes,
        ) : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTUnion(this)

            override fun read(reader: BeamReader): TUnion = reader.readShapeTUnion()

            override fun toString(): String = toString(this)
        }

        public object TDynamic : Shape {

            override fun write(writer: BeamWriter): Unit = writer.writeShapeTDynamic(this)

            override fun read(reader: BeamReader): TDynamic = reader.readShapeTDynamic()

            override fun toString(): String = toString(this)
        }
    }

    public class Shapes(private val items: ArrayList<Shape>) : RidlList<Shape>(items), `$Serializable` {

        override fun write(writer: BeamWriter): Unit = writer.writeShapes(this)

        override fun read(reader: BeamReader): Shapes = reader.readShapes()

        override fun toString(): String = toString(this)
    }

    public class Fields(private val items: ArrayList<Field>) : RidlList<Field>(items), `$Serializable` {

        override fun write(writer: BeamWriter): Unit = writer.writeFields(this)

        override fun read(reader: BeamReader): Fields = reader.readFields()

        override fun toString(): String = toString(this)
    }

    public data class Field(
        @JvmField var name: String,
        @JvmField var shape: Shape,
    ) : `$Serializable` {

        override fun write(writer: BeamWriter): Unit = writer.writeField(this)

        override fun read(reader: BeamReader): Field = reader.readField()

        override fun toString(): String = toString(this)
    }
}
