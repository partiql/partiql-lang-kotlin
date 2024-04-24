package org.partiql.types.shape

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.IonWriter

public interface IonSerializable {
    public fun write(writer: IonWriter)
}

public abstract class _Array<T>(private val items: Array<T>) : IonSerializable {
    public val size: Int = items.size
    public operator fun get(index: Int): T = items[index]
    public operator fun set(index: Int, value: T): Unit = items.set(index, value)
    public operator fun iterator(): Iterator<T> = items.iterator()
}

public abstract class _ArrayList<T>(private val items: ArrayList<T>) : IonSerializable {
    public val size: Int = items.size
    public operator fun get(index: Int): T = items[index]
    public operator fun set(index: Int, value: T): T = items.set(index, value)
    public operator fun iterator(): Iterator<T> = items.iterator()
}

public sealed interface Shape : IonSerializable {

    public object TBool : Shape {

        public const val TAG: Long = 0

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TBool {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TBool
        }
    }

    public object TInt8 : Shape {

        public const val TAG: Long = 1

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TInt8 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TInt8
        }
    }

    public object TInt16 : Shape {

        public const val TAG: Long = 2

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TInt16 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TInt16
        }
    }

    public object TInt32 : Shape {

        public const val TAG: Long = 3

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TInt32 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TInt32
        }
    }

    public object TInt64 : Shape {

        public const val TAG: Long = 4

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TInt64 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TInt64
        }
    }

    public object TInteger : Shape {

        public const val TAG: Long = 5

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TInteger {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TInteger
        }
    }

    public object TDecimal : Shape {

        public const val TAG: Long = 6

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TDecimal {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TDecimal
        }
    }

    public data class TNumeric(
        @JvmField val precision: Int,
        @JvmField val scale: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.writeInt(scale.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 7

            @JvmStatic
            public fun read(reader: IonReader): TNumeric {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == IonType.INT)
                val scale: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TNumeric(precision, scale)
            }
        }
    }

    public object TFloat32 : Shape {

        public const val TAG: Long = 8

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TFloat32 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TFloat32
        }
    }

    public object TFloat64 : Shape {

        public const val TAG: Long = 9

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TFloat64 {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TFloat64
        }
    }

    public data class TFloat(
        @JvmField val precision: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 10

            @JvmStatic
            public fun read(reader: IonReader): TFloat {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TFloat(precision)
            }
        }
    }

    public data class TCharFixed(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 11

            @JvmStatic
            public fun read(reader: IonReader): TCharFixed {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TCharFixed(length)
            }
        }
    }

    public data class TCharVarying(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 12

            @JvmStatic
            public fun read(reader: IonReader): TCharVarying {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TCharVarying(length)
            }
        }
    }

    public object TString : Shape {

        public const val TAG: Long = 13

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TString {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TString
        }
    }

    public object TSymbol : Shape {

        public const val TAG: Long = 14

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TSymbol {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TSymbol
        }
    }

    public object TClob : Shape {

        public const val TAG: Long = 15

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TClob {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TClob
        }
    }

    public data class TBitFixed(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 16

            @JvmStatic
            public fun read(reader: IonReader): TBitFixed {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TBitFixed(length)
            }
        }
    }

    public data class TBitVarying(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 17

            @JvmStatic
            public fun read(reader: IonReader): TBitVarying {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TBitVarying(length)
            }
        }
    }

    public object TBinary : Shape {

        public const val TAG: Long = 18

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TBinary {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TBinary
        }
    }

    public data class TByteFixed(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 19

            @JvmStatic
            public fun read(reader: IonReader): TByteFixed {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TByteFixed(length)
            }
        }
    }

    public data class TByteVarying(
        @JvmField val length: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(length.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 20

            @JvmStatic
            public fun read(reader: IonReader): TByteVarying {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val length: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TByteVarying(length)
            }
        }
    }

    public object TBlob : Shape {

        public const val TAG: Long = 21

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TBlob {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TBlob
        }
    }

    public object TDate : Shape {

        public const val TAG: Long = 22

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TDate {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TDate
        }
    }

    public data class TTime(
        @JvmField val precision: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 23

            @JvmStatic
            public fun read(reader: IonReader): TTime {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TTime(precision)
            }
        }
    }

    public data class TTimeTz(
        @JvmField val precision: Int,
        @JvmField val offset_hour: Int,
        @JvmField val offset_minute: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.writeInt(offset_hour.toLong())
            writer.writeInt(offset_minute.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 24

            @JvmStatic
            public fun read(reader: IonReader): TTimeTz {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == IonType.INT)
                val offset_hour: Int = reader.intValue()
                assert(reader.next() == IonType.INT)
                val offset_minute: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TTimeTz(precision, offset_hour, offset_minute)
            }
        }
    }

    public data class TTimestamp(
        @JvmField val precision: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 25

            @JvmStatic
            public fun read(reader: IonReader): TTimestamp {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TTimestamp(precision)
            }
        }
    }

    public data class TTimestampTz(
        @JvmField val precision: Int,
        @JvmField val offset_hour: Int,
        @JvmField val offset_minute: Int,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(precision.toLong())
            writer.writeInt(offset_hour.toLong())
            writer.writeInt(offset_minute.toLong())
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 26

            @JvmStatic
            public fun read(reader: IonReader): TTimestampTz {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.INT)
                val precision: Int = reader.intValue()
                assert(reader.next() == IonType.INT)
                val offset_hour: Int = reader.intValue()
                assert(reader.next() == IonType.INT)
                val offset_minute: Int = reader.intValue()
                assert(reader.next() == null)
                reader.stepOut()
                return TTimestampTz(precision, offset_hour, offset_minute)
            }
        }
    }

    public data class TBag(
        @JvmField val items: Shape,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            items.write(writer)
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 27

            @JvmStatic
            public fun read(reader: IonReader): TBag {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.SEXP)
                val items: Shape = Shape.read(reader)
                assert(reader.next() == null)
                reader.stepOut()
                return TBag(items)
            }
        }
    }

    public data class TList(
        @JvmField val items: Shape,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            items.write(writer)
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 28

            @JvmStatic
            public fun read(reader: IonReader): TList {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.SEXP)
                val items: Shape = Shape.read(reader)
                assert(reader.next() == null)
                reader.stepOut()
                return TList(items)
            }
        }
    }

    public data class TSexp(
        @JvmField val items: Shape,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            items.write(writer)
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 29

            @JvmStatic
            public fun read(reader: IonReader): TSexp {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.SEXP)
                val items: Shape = Shape.read(reader)
                assert(reader.next() == null)
                reader.stepOut()
                return TSexp(items)
            }
        }
    }

    public data class TStruct(
        @JvmField val fields: Fields,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            fields.write(writer)
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 30

            @JvmStatic
            public fun read(reader: IonReader): TStruct {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.SEXP)
                val fields: Fields = Fields.read(reader)
                assert(reader.next() == null)
                reader.stepOut()
                return TStruct(fields)
            }
        }
    }

    public object TNull : Shape {

        public const val TAG: Long = 31

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TNull {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TNull
        }
    }

    public object TMissing : Shape {

        public const val TAG: Long = 32

        public override fun write(writer: IonWriter) {
            writer.writeSymbol("unit")
        }

        @JvmStatic
        public fun read(reader: IonReader): TMissing {
            assert(reader.type == IonType.SYMBOL)
            assert(reader.symbolValue().text == "unit")
            return TMissing
        }
    }

    public data class TUnion(
        @JvmField val shapes: Shapes,
    ) : Shape {

        public override fun write(writer: IonWriter) {
            Shape.stepIn(writer)
            writer.stepIn(IonType.SEXP)
            writer.writeInt(TAG)
            writer.stepIn(IonType.SEXP)
            shapes.write(writer)
            writer.stepOut()
            writer.stepOut()
            Shape.stepOut(writer)
        }

        public companion object {

            public const val TAG: Long = 33

            @JvmStatic
            public fun read(reader: IonReader): TUnion {
                assert(reader.type == IonType.SEXP)
                reader.stepIn()
                assert(reader.next() == IonType.SEXP)
                val shapes: Shapes = Shapes.read(reader)
                assert(reader.next() == null)
                reader.stepOut()
                return TUnion(shapes)
            }
        }
    }

    public companion object {

        internal fun stepIn(writer: IonWriter) {
            // no-op
        }

        internal fun stepOut(writer: IonWriter) {
            // no-op
        }

        @JvmStatic
        public fun read(reader: IonReader): Shape {
            assert(reader.type == IonType.SEXP)
            reader.stepIn()
            assert(reader.next() == IonType.INT)
            val tag: Long = reader.longValue()
            reader.next()
            val variant = when (tag) {
                TBool.TAG -> TBool.read(reader)
                TInt8.TAG -> TInt8.read(reader)
                TInt16.TAG -> TInt16.read(reader)
                TInt32.TAG -> TInt32.read(reader)
                TInt64.TAG -> TInt64.read(reader)
                TInteger.TAG -> TInteger.read(reader)
                TDecimal.TAG -> TDecimal.read(reader)
                TNumeric.TAG -> TNumeric.read(reader)
                TFloat32.TAG -> TFloat32.read(reader)
                TFloat64.TAG -> TFloat64.read(reader)
                TFloat.TAG -> TFloat.read(reader)
                TCharFixed.TAG -> TCharFixed.read(reader)
                TCharVarying.TAG -> TCharVarying.read(reader)
                TString.TAG -> TString.read(reader)
                TSymbol.TAG -> TSymbol.read(reader)
                TClob.TAG -> TClob.read(reader)
                TBitFixed.TAG -> TBitFixed.read(reader)
                TBitVarying.TAG -> TBitVarying.read(reader)
                TBinary.TAG -> TBinary.read(reader)
                TByteFixed.TAG -> TByteFixed.read(reader)
                TByteVarying.TAG -> TByteVarying.read(reader)
                TBlob.TAG -> TBlob.read(reader)
                TDate.TAG -> TDate.read(reader)
                TTime.TAG -> TTime.read(reader)
                TTimeTz.TAG -> TTimeTz.read(reader)
                TTimestamp.TAG -> TTimestamp.read(reader)
                TTimestampTz.TAG -> TTimestampTz.read(reader)
                TBag.TAG -> TBag.read(reader)
                TList.TAG -> TList.read(reader)
                TSexp.TAG -> TSexp.read(reader)
                TStruct.TAG -> TStruct.read(reader)
                TNull.TAG -> TNull.read(reader)
                TMissing.TAG -> TMissing.read(reader)
                TUnion.TAG -> TUnion.read(reader)
                else -> error("Invalid tag `$tag` on union type `Shape`")
            }
            reader.stepOut()
            return variant
        }
    }
}

public class Shapes(private val items: ArrayList<Shape>) : _ArrayList<Shape>(items), IonSerializable {

    override fun write(writer: IonWriter) {
        writer.stepIn(IonType.LIST)
        for (item in items) {
            item.write(writer)
        }
        writer.stepOut()
    }

    public companion object {

        @JvmStatic
        public fun read(reader: IonReader): Shapes {
            val items = arrayListOf<Shape>()
            assert(reader.type == IonType.LIST)
            reader.stepIn()
            while (reader.next() == IonType.SEXP) {
                items.add(Shape.read(reader))
            }
            reader.stepOut()
            return Shapes(items)
        }
    }
}

public class Fields(private val items: ArrayList<Field>) : _ArrayList<Field>(items), IonSerializable {

    override fun write(writer: IonWriter) {
        writer.stepIn(IonType.LIST)
        for (item in items) {
            item.write(writer)
        }
        writer.stepOut()
    }

    public companion object {

        @JvmStatic
        public fun read(reader: IonReader): Fields {
            val items = arrayListOf<Field>()
            assert(reader.type == IonType.LIST)
            reader.stepIn()
            while (reader.next() == IonType.SEXP) {
                items.add(Field.read(reader))
            }
            reader.stepOut()
            return Fields(items)
        }
    }
}

public data class Field(
    @JvmField val k: String,
    @JvmField val v: Shape,
) : IonSerializable {

    public override fun write(writer: IonWriter) {
        writer.stepIn(IonType.SEXP)
        writer.writeString(k)
        v.write(writer)
        writer.stepOut()
    }

    public companion object {

        @JvmStatic
        public fun read(reader: IonReader): Field {
            assert(reader.type == IonType.SEXP)
            reader.stepIn()
            assert(reader.next() == IonType.STRING)
            val k: String = reader.stringValue()
            assert(reader.next() == IonType.SEXP)
            val v: Shape = Shape.read(reader)
            assert(reader.next() == null)
            reader.stepOut()
            return Field(k, v)
        }
    }
}

