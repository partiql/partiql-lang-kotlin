package org.partiql.beam.io

import com.amazon.ion.IonReader
import org.partiql.beam.io.runtime.RidlReader

public abstract class BeamReader(reader: IonReader) : RidlReader(reader) {

    public fun read(value: Beam.`$Serializable`): Beam.`$Serializable` = value.read(this)

    public abstract fun readShape(): Beam.Shape

    public abstract fun readShapeTBool(): Beam.Shape.TBool

    public abstract fun readShapeTInt8(): Beam.Shape.TInt8

    public abstract fun readShapeTInt16(): Beam.Shape.TInt16

    public abstract fun readShapeTInt32(): Beam.Shape.TInt32

    public abstract fun readShapeTInt64(): Beam.Shape.TInt64

    public abstract fun readShapeTInteger(): Beam.Shape.TInteger

    public abstract fun readShapeTDecimal(): Beam.Shape.TDecimal

    public abstract fun readShapeTNumeric(): Beam.Shape.TNumeric

    public abstract fun readShapeTFloat32(): Beam.Shape.TFloat32

    public abstract fun readShapeTFloat64(): Beam.Shape.TFloat64

    public abstract fun readShapeTFloat(): Beam.Shape.TFloat

    public abstract fun readShapeTCharFixed(): Beam.Shape.TCharFixed

    public abstract fun readShapeTCharVarying(): Beam.Shape.TCharVarying

    public abstract fun readShapeTString(): Beam.Shape.TString

    public abstract fun readShapeTSymbol(): Beam.Shape.TSymbol

    public abstract fun readShapeTClob(): Beam.Shape.TClob

    public abstract fun readShapeTBitFixed(): Beam.Shape.TBitFixed

    public abstract fun readShapeTBitVarying(): Beam.Shape.TBitVarying

    public abstract fun readShapeTBinary(): Beam.Shape.TBinary

    public abstract fun readShapeTByteFixed(): Beam.Shape.TByteFixed

    public abstract fun readShapeTByteVarying(): Beam.Shape.TByteVarying

    public abstract fun readShapeTBlob(): Beam.Shape.TBlob

    public abstract fun readShapeTDate(): Beam.Shape.TDate

    public abstract fun readShapeTTime(): Beam.Shape.TTime

    public abstract fun readShapeTTimeTz(): Beam.Shape.TTimeTz

    public abstract fun readShapeTTimestamp(): Beam.Shape.TTimestamp

    public abstract fun readShapeTTimestampTz(): Beam.Shape.TTimestampTz

    public abstract fun readShapeTBag(): Beam.Shape.TBag

    public abstract fun readShapeTList(): Beam.Shape.TList

    public abstract fun readShapeTSexp(): Beam.Shape.TSexp

    public abstract fun readShapeTStruct(): Beam.Shape.TStruct

    public abstract fun readShapeTNull(): Beam.Shape.TNull

    public abstract fun readShapeTMissing(): Beam.Shape.TMissing

    public abstract fun readShapeTUnion(): Beam.Shape.TUnion

    public abstract fun readShapeTDynamic(): Beam.Shape.TDynamic

    public abstract fun readShapes(): Beam.Shapes

    public abstract fun readFields(): Beam.Fields

    public abstract fun readField(): Beam.Field

    public companion object {

        @JvmStatic
        public fun text(reader: IonReader): BeamReader = Text(reader)

        @JvmStatic
        public fun packed(reader: IonReader): BeamReader = Packed(reader)
    }

    private class Text(private val reader: IonReader) : BeamReader(reader) {

        override fun readShape(): Beam.Shape = with(reader) {
            TODO()
        }

        override fun readShapeTBool(): Beam.Shape.TBool = with(reader) {
            TODO()
        }

        override fun readShapeTInt8(): Beam.Shape.TInt8 = with(reader) {
            TODO()
        }

        override fun readShapeTInt16(): Beam.Shape.TInt16 = with(reader) {
            TODO()
        }

        override fun readShapeTInt32(): Beam.Shape.TInt32 = with(reader) {
            TODO()
        }

        override fun readShapeTInt64(): Beam.Shape.TInt64 = with(reader) {
            TODO()
        }

        override fun readShapeTInteger(): Beam.Shape.TInteger = with(reader) {
            TODO()
        }

        override fun readShapeTDecimal(): Beam.Shape.TDecimal = with(reader) {
            TODO()
        }

        override fun readShapeTNumeric(): Beam.Shape.TNumeric = with(reader) {
            TODO()
        }

        override fun readShapeTFloat32(): Beam.Shape.TFloat32 = with(reader) {
            TODO()
        }

        override fun readShapeTFloat64(): Beam.Shape.TFloat64 = with(reader) {
            TODO()
        }

        override fun readShapeTFloat(): Beam.Shape.TFloat = with(reader) {
            TODO()
        }

        override fun readShapeTCharFixed(): Beam.Shape.TCharFixed = with(reader) {
            TODO()
        }

        override fun readShapeTCharVarying(): Beam.Shape.TCharVarying = with(reader) {
            TODO()
        }

        override fun readShapeTString(): Beam.Shape.TString = with(reader) {
            TODO()
        }

        override fun readShapeTSymbol(): Beam.Shape.TSymbol = with(reader) {
            TODO()
        }

        override fun readShapeTClob(): Beam.Shape.TClob = with(reader) {
            TODO()
        }

        override fun readShapeTBitFixed(): Beam.Shape.TBitFixed = with(reader) {
            TODO()
        }

        override fun readShapeTBitVarying(): Beam.Shape.TBitVarying = with(reader) {
            TODO()
        }

        override fun readShapeTBinary(): Beam.Shape.TBinary = with(reader) {
            TODO()
        }

        override fun readShapeTByteFixed(): Beam.Shape.TByteFixed = with(reader) {
            TODO()
        }

        override fun readShapeTByteVarying(): Beam.Shape.TByteVarying = with(reader) {
            TODO()
        }

        override fun readShapeTBlob(): Beam.Shape.TBlob = with(reader) {
            TODO()
        }

        override fun readShapeTDate(): Beam.Shape.TDate = with(reader) {
            TODO()
        }

        override fun readShapeTTime(): Beam.Shape.TTime = with(reader) {
            TODO()
        }

        override fun readShapeTTimeTz(): Beam.Shape.TTimeTz = with(reader) {
            TODO()
        }

        override fun readShapeTTimestamp(): Beam.Shape.TTimestamp = with(reader) {
            TODO()
        }

        override fun readShapeTTimestampTz(): Beam.Shape.TTimestampTz = with(reader) {
            TODO()
        }

        override fun readShapeTBag(): Beam.Shape.TBag = with(reader) {
            TODO()
        }

        override fun readShapeTList(): Beam.Shape.TList = with(reader) {
            TODO()
        }

        override fun readShapeTSexp(): Beam.Shape.TSexp = with(reader) {
            TODO()
        }

        override fun readShapeTStruct(): Beam.Shape.TStruct = with(reader) {
            TODO()
        }

        override fun readShapeTNull(): Beam.Shape.TNull = with(reader) {
            TODO()
        }

        override fun readShapeTMissing(): Beam.Shape.TMissing = with(reader) {
            TODO()
        }

        override fun readShapeTUnion(): Beam.Shape.TUnion = with(reader) {
            TODO()
        }

        override fun readShapeTDynamic(): Beam.Shape.TDynamic = with(reader) {
            TODO()
        }

        override fun readShapes(): Beam.Shapes = with(reader) {
            TODO()
        }

        override fun readFields(): Beam.Fields = with(reader) {
            TODO()
        }

        override fun readField(): Beam.Field = with(reader) {
            TODO()
        }
    }

    private class Packed(private val reader: IonReader) : BeamReader(reader) {

        override fun readShape(): Beam.Shape = with(reader) {
            TODO()
        }

        override fun readShapeTBool(): Beam.Shape.TBool = with(reader) {
            TODO()
        }

        override fun readShapeTInt8(): Beam.Shape.TInt8 = with(reader) {
            TODO()
        }

        override fun readShapeTInt16(): Beam.Shape.TInt16 = with(reader) {
            TODO()
        }

        override fun readShapeTInt32(): Beam.Shape.TInt32 = with(reader) {
            TODO()
        }

        override fun readShapeTInt64(): Beam.Shape.TInt64 = with(reader) {
            TODO()
        }

        override fun readShapeTInteger(): Beam.Shape.TInteger = with(reader) {
            TODO()
        }

        override fun readShapeTDecimal(): Beam.Shape.TDecimal = with(reader) {
            TODO()
        }

        override fun readShapeTNumeric(): Beam.Shape.TNumeric = with(reader) {
            TODO()
        }

        override fun readShapeTFloat32(): Beam.Shape.TFloat32 = with(reader) {
            TODO()
        }

        override fun readShapeTFloat64(): Beam.Shape.TFloat64 = with(reader) {
            TODO()
        }

        override fun readShapeTFloat(): Beam.Shape.TFloat = with(reader) {
            TODO()
        }

        override fun readShapeTCharFixed(): Beam.Shape.TCharFixed = with(reader) {
            TODO()
        }

        override fun readShapeTCharVarying(): Beam.Shape.TCharVarying = with(reader) {
            TODO()
        }

        override fun readShapeTString(): Beam.Shape.TString = with(reader) {
            TODO()
        }

        override fun readShapeTSymbol(): Beam.Shape.TSymbol = with(reader) {
            TODO()
        }

        override fun readShapeTClob(): Beam.Shape.TClob = with(reader) {
            TODO()
        }

        override fun readShapeTBitFixed(): Beam.Shape.TBitFixed = with(reader) {
            TODO()
        }

        override fun readShapeTBitVarying(): Beam.Shape.TBitVarying = with(reader) {
            TODO()
        }

        override fun readShapeTBinary(): Beam.Shape.TBinary = with(reader) {
            TODO()
        }

        override fun readShapeTByteFixed(): Beam.Shape.TByteFixed = with(reader) {
            TODO()
        }

        override fun readShapeTByteVarying(): Beam.Shape.TByteVarying = with(reader) {
            TODO()
        }

        override fun readShapeTBlob(): Beam.Shape.TBlob = with(reader) {
            TODO()
        }

        override fun readShapeTDate(): Beam.Shape.TDate = with(reader) {
            TODO()
        }

        override fun readShapeTTime(): Beam.Shape.TTime = with(reader) {
            TODO()
        }

        override fun readShapeTTimeTz(): Beam.Shape.TTimeTz = with(reader) {
            TODO()
        }

        override fun readShapeTTimestamp(): Beam.Shape.TTimestamp = with(reader) {
            TODO()
        }

        override fun readShapeTTimestampTz(): Beam.Shape.TTimestampTz = with(reader) {
            TODO()
        }

        override fun readShapeTBag(): Beam.Shape.TBag = with(reader) {
            TODO()
        }

        override fun readShapeTList(): Beam.Shape.TList = with(reader) {
            TODO()
        }

        override fun readShapeTSexp(): Beam.Shape.TSexp = with(reader) {
            TODO()
        }

        override fun readShapeTStruct(): Beam.Shape.TStruct = with(reader) {
            TODO()
        }

        override fun readShapeTNull(): Beam.Shape.TNull = with(reader) {
            TODO()
        }

        override fun readShapeTMissing(): Beam.Shape.TMissing = with(reader) {
            TODO()
        }

        override fun readShapeTUnion(): Beam.Shape.TUnion = with(reader) {
            TODO()
        }

        override fun readShapeTDynamic(): Beam.Shape.TDynamic = with(reader) {
            TODO()
        }

        override fun readShapes(): Beam.Shapes = with(reader) {
            TODO()
        }

        override fun readFields(): Beam.Fields = with(reader) {
            TODO()
        }

        override fun readField(): Beam.Field = with(reader) {
            TODO()
        }
    }
}
