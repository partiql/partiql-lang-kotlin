package org.partiql.value.io

import com.amazon.ion.Decimal
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ion.system.IonWriterBuilder
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionBlob
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionClob
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.ionTimestamp
import org.partiql.value.BagValue
import org.partiql.value.BinaryValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.ByteValue
import org.partiql.value.CharValue
import org.partiql.value.ClobValue
import org.partiql.value.CollectionValue
import org.partiql.value.DateValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.IntervalValue
import org.partiql.value.ListValue
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.NullableBagValue
import org.partiql.value.NullableBinaryValue
import org.partiql.value.NullableBlobValue
import org.partiql.value.NullableBoolValue
import org.partiql.value.NullableByteValue
import org.partiql.value.NullableCharValue
import org.partiql.value.NullableClobValue
import org.partiql.value.NullableDateValue
import org.partiql.value.NullableDecimalValue
import org.partiql.value.NullableFloat32Value
import org.partiql.value.NullableFloat64Value
import org.partiql.value.NullableInt16Value
import org.partiql.value.NullableInt32Value
import org.partiql.value.NullableInt64Value
import org.partiql.value.NullableInt8Value
import org.partiql.value.NullableIntValue
import org.partiql.value.NullableIntervalValue
import org.partiql.value.NullableListValue
import org.partiql.value.NullableScalarValue
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableStructValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.NullableTimeValue
import org.partiql.value.NullableTimestampValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.ScalarValue
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.binaryValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.byteValue
import org.partiql.value.charValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.io.PartiQLValueIonWriter.ToIon.toIon
import org.partiql.value.stringValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import org.partiql.value.util.PartiQLValueBaseVisitor
import java.io.OutputStream

@OptIn(PartiQLValueExperimental::class)
internal class PartiQLValueIonWriter(
    private val ionWriter: IonWriter,
) : PartiQLValueWriter {

    companion object {
        const val MISSING_ANNOTATION = "\$missing"
        const val BAG_ANNOTATION = "\$bag"
        const val DATE_ANNOTATION = "\$date"
        const val TIME_ANNOTATION = "\$time"
        // PartiQL's timestamp without time zone does not fit in ion generic timestamp
        const val TIMESTAMP_ANNOTATION = "\$timestamp"
        const val GRAPH_ANNOTATION = "\$graph"
    }

    override fun append(value: PartiQLValue): PartiQLValueWriter {
        ToIon.visit(value, Unit).writeTo(ionWriter)
        return this
    }

    override fun close() {
        ionWriter.close()
    }

    private object ToIon : PartiQLValueBaseVisitor<IonElement, Unit>() {

        override fun defaultVisit(v: PartiQLValue, ctx: Unit) = defaultReturn(v, ctx)

        override fun defaultReturn(v: PartiQLValue, ctx: Unit): Nothing =
            throw IllegalArgumentException("Cannot represent $v as Ion")

        private inline fun <T> ScalarValue<T>.toIon(block: ScalarValue<T>.() -> IonElement): IonElement {
            val e = this.block()
            return e.withAnnotations(this.annotations)
        }

        private inline fun <T> NullableScalarValue<T>.toIon(block: NullableScalarValue<T>.() -> IonElement): IonElement {
            val e = this.block()
            return e.withAnnotations(this.annotations)
        }

        private inline fun CollectionValue<*>.toIon(block: CollectionValue<*>.(elements: List<IonElement>) -> IonElement): IonElement {
            val elements = this.elements.map { it.accept(ToIon, Unit) }
            val e = this.block(elements)
            return e.withAnnotations(this.annotations)
        }

        override fun visitNull(v: NullValue, ctx: Unit): IonElement = ionNull().withAnnotations(v.annotations)

        override fun visitMissing(v: MissingValue, ctx: Unit): IonElement = ionNull().withAnnotations(
            v.annotations + listOf(
                MISSING_ANNOTATION
            )
        )

        override fun visitBool(v: BoolValue, ctx: Unit): IonElement = v.toIon { ionBool(value) }

        override fun visitNullableBool(v: NullableBoolValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.BOOL) }
            else -> visitBool(boolValue(v.value!!, v.annotations), ctx)
        }

        override fun visitInt8(v: Int8Value, ctx: Unit): IonElement = v.toIon { ionInt(value.toLong()) }

        override fun visitNullableInt8(v: NullableInt8Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.INT) }
            else -> visitInt8(int8Value(v.value!!, v.annotations), ctx)
        }

        override fun visitInt16(v: Int16Value, ctx: Unit): IonElement = v.toIon { ionInt(value.toLong()) }

        override fun visitNullableInt16(v: NullableInt16Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.INT) }
            else -> visitInt16(int16Value(v.value!!, v.annotations), ctx)
        }

        override fun visitInt32(v: Int32Value, ctx: Unit): IonElement = v.toIon { ionInt(value.toLong()) }

        override fun visitNullableInt32(v: NullableInt32Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.INT) }
            else -> visitInt32(int32Value(v.value!!, v.annotations), ctx)
        }

        override fun visitInt64(v: Int64Value, ctx: Unit): IonElement = v.toIon { ionInt(value) }

        override fun visitNullableInt64(v: NullableInt64Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.INT) }
            else -> visitInt64(int64Value(v.value!!, v.annotations), ctx)
        }

        override fun visitInt(v: IntValue, ctx: Unit): IonElement = v.toIon { ionInt(value.toLong()) }

        override fun visitNullableInt(v: NullableIntValue, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.INT) }
            else -> visitInt(intValue(v.value!!, v.annotations), ctx)
        }

        override fun visitDecimal(v: DecimalValue, ctx: Unit): IonElement =
            v.toIon { ionDecimal(Decimal.valueOf(value)) }

        override fun visitNullableDecimal(v: NullableDecimalValue, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.DECIMAL) }
            else -> visitDecimal(decimalValue(v.value!!, v.annotations), ctx)
        }

        // TODO : This is wrong. Ion float is 64 bit, when we cast the 32 bit float to double, we deliver false promise
        override fun visitFloat32(v: Float32Value, ctx: Unit): IonElement =
            v.toIon { ionFloat(value.toString().toDouble()) }

        override fun visitNullableFloat32(v: NullableFloat32Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.FLOAT) }

            else -> visitFloat32(float32Value(v.value!!, v.annotations), ctx)
        }

        override fun visitFloat64(v: Float64Value, ctx: Unit): IonElement = v.toIon { ionFloat(value) }

        override fun visitNullableFloat64(v: NullableFloat64Value, ctx: Unit): IonElement = when (v.value) {
            null -> v.toIon { ionNull(ElementType.FLOAT) }
            else -> visitFloat64(float64Value(v.value!!, v.annotations), ctx)
        }

        // TODO: Revisit
        override fun visitChar(v: CharValue, ctx: Unit): IonElement = v.toIon { ionString(value.toString()) }

        override fun visitNullableChar(v: NullableCharValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.STRING) }
            else -> visitChar(charValue(v.value!!, v.annotations), ctx)
        }

        override fun visitString(v: StringValue, ctx: Unit): IonElement = v.toIon { ionString(value) }

        override fun visitNullableString(v: NullableStringValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.STRING) }
            else -> visitString(stringValue(v.value!!, v.annotations), ctx)
        }

        override fun visitSymbol(v: SymbolValue, ctx: Unit): IonElement = v.toIon { ionSymbol(value) }

        override fun visitNullableSymbol(v: NullableSymbolValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.SYMBOL) }
            else -> visitSymbol(symbolValue(v.value!!, v.annotations), ctx)
        }

        override fun visitClob(v: ClobValue, ctx: Unit): IonElement = v.toIon { ionClob(value) }

        override fun visitNullableClob(v: NullableClobValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.CLOB) }
            else -> visitClob(clobValue(v.value!!, v.annotations), ctx)
        }

        // TODO: Revisit
        override fun visitBinary(v: BinaryValue, ctx: Unit): IonElement = v.toIon { ionBlob(value.toByteArray()) }

        override fun visitNullableBinary(v: NullableBinaryValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.BLOB) }
            else -> visitBinary(binaryValue(v.value!!, v.annotations), ctx)
        }

        // TODO: Revisit
        override fun visitByte(v: ByteValue, ctx: Unit): IonElement = v.toIon { ionBlob(ByteArray(1) { value }) }

        override fun visitNullableByte(v: NullableByteValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.BLOB) }
            else -> visitByte(byteValue(v.value!!, v.annotations), ctx)
        }

        override fun visitBlob(v: BlobValue, ctx: Unit): IonElement = v.toIon { ionBlob(v.value) }

        override fun visitNullableBlob(v: NullableBlobValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.BLOB) }
            else -> visitBlob(blobValue(v.value!!, v.annotations), ctx)
        }

        override fun visitDate(v: DateValue, ctx: Unit): IonElement = v.toIon {
            val date = v.value
            ionStructOf(
                field("year", ionInt(date.year.toLong())),
                field("month", ionInt(date.month.toLong())),
                field("day", ionInt(date.day.toLong()))
            )
        }.withAnnotations(DATE_ANNOTATION)

        override fun visitNullableDate(v: NullableDateValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.STRUCT) }
            else -> visitDate(dateValue(v.value!!, v.annotations), ctx)
        }

        override fun visitTime(v: TimeValue, ctx: Unit): IonElement =
            v.toIon {
                when (val timeZone = v.value.timeZone) {
                    TimeZone.UnknownTimeZone ->
                        ionStructOf(
                            field("hour", ionInt(v.value.hour.toLong())),
                            field("minute", ionInt(v.value.minute.toLong())),
                            field("second", ionDecimal(Decimal.valueOf(v.value.second))),
                            field("offset", ionNull(ElementType.INT)),
                        )
                    is TimeZone.UtcOffset ->
                        ionStructOf(
                            field("hour", ionInt(v.value.hour.toLong())),
                            field("minute", ionInt(v.value.minute.toLong())),
                            field("second", ionDecimal(Decimal.valueOf(v.value.second))),
                            field("offset", ionInt(timeZone.totalOffsetMinutes.toLong()))
                        )
                    null ->
                        ionStructOf(
                            field("hour", ionInt(v.value.hour.toLong())),
                            field("minute", ionInt(v.value.minute.toLong())),
                            field("second", ionDecimal(Decimal.valueOf(v.value.second)))
                        )
                }
            }.withAnnotations(TIME_ANNOTATION)

        override fun visitNullableTime(v: NullableTimeValue, ctx: Unit) = when (v.value) {
            null -> v.toIon { ionNull(ElementType.STRUCT) }.withAnnotations(TIME_ANNOTATION)
            else -> visitTime(timeValue(v.value!!, v.annotations), ctx)
        }

        override fun visitTimestamp(v: TimestampValue, ctx: Unit): IonElement =
            when (v.value.timeZone) {
                TimeZone.UnknownTimeZone, is TimeZone.UtcOffset -> v.toIon { ionTimestamp(v.value.ionTimestampValue) }
                null -> v.toIon {
                    ionStructOf(
                        field("year", ionInt(v.value.year.toLong())),
                        field("month", ionInt(v.value.month.toLong())),
                        field("day", ionInt(v.value.day.toLong())),
                        field("hour", ionInt(v.value.hour.toLong())),
                        field("minute", ionInt(v.value.minute.toLong())),
                        field("second", ionDecimal(Decimal.valueOf(v.value.second)))
                    )
                }.withAnnotations(TIMESTAMP_ANNOTATION)
            }

        override fun visitNullableTimestamp(v: NullableTimestampValue, ctx: Unit) = when (v.value) {
            // TODO: we actually don't know if this is a timestamp with timezone or timestamp without timezone
            //  Should we care?
            null -> v.toIon { ionNull(ElementType.TIMESTAMP) }
            else -> visitTimestamp(timestampValue(v.value!!, v.annotations), ctx)
        }

        override fun visitInterval(v: IntervalValue, ctx: Unit): IonElement = TODO("Not Yet supported")

        override fun visitNullableInterval(v: NullableIntervalValue, ctx: Unit) = TODO("Not yet supported")

        override fun visitBag(v: BagValue<*>, ctx: Unit): IonElement =
            v.toIon { elements -> ionListOf(elements) }.withAnnotations(BAG_ANNOTATION)

        override fun visitNullableBag(v: NullableBagValue<*>, ctx: Unit) = when (v.isNull()) {
            true -> ionNull(ElementType.LIST).withAnnotations(v.annotations).withAnnotations(BAG_ANNOTATION)
            false -> visitBag(v.promote(), ctx)
        }

        override fun visitList(v: ListValue<*>, ctx: Unit): IonElement = v.toIon { elements -> ionListOf(elements) }

        override fun visitNullableList(v: NullableListValue<*>, ctx: Unit) = when (v.isNull()) {
            true -> ionNull(ElementType.LIST).withAnnotations(v.annotations)
            false -> visitList(v.promote(), ctx)
        }

        override fun visitSexp(v: SexpValue<*>, ctx: Unit): IonElement = v.toIon { elements -> ionSexpOf(elements) }

        override fun visitNullableSexp(v: NullableSexpValue<*>, ctx: Unit) =
            when (v.isNull()) {
                true -> ionNull(ElementType.SEXP).withAnnotations(v.annotations)
                false -> visitSexp(v.promote(), ctx)
            }

        override fun visitStruct(v: StructValue<*>, ctx: Unit): IonElement {
            val fields = v.fields.map {
                val k = it.first
                val v = it.second.accept(this, ctx)
                field(k, v)
            }
            return ionStructOf(fields, v.annotations)
        }

        override fun visitNullableStruct(v: NullableStructValue<*>, ctx: Unit): IonElement = when (v.isNull()) {
            true -> ionNull(ElementType.STRUCT, v.annotations)
            false -> visitStruct(v.promote(), ctx)
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
public class PartiQLValueIonWriterBuilder private constructor(
    private var ionWriterBuilder: IonWriterBuilder,
) {

    public companion object {
        @JvmStatic
        public fun standardIonTextBuilder(): PartiQLValueIonWriterBuilder = PartiQLValueIonWriterBuilder(
            ionWriterBuilder = IonTextWriterBuilder.standard()
        )

        @JvmStatic
        public fun standardIonBinaryBuilder(): PartiQLValueIonWriterBuilder = PartiQLValueIonWriterBuilder(
            ionWriterBuilder = IonBinaryWriterBuilder.standard()
        )
    }

    public fun build(output: OutputStream): PartiQLValueWriter =
        PartiQLValueIonWriter(
            ionWriter = ionWriterBuilder.build(output),
        )

    public fun ionWriterBuilder(ionWriterBuilder: IonWriterBuilder): PartiQLValueIonWriterBuilder = this.apply {
        this.ionWriterBuilder = ionWriterBuilder
    }
}
