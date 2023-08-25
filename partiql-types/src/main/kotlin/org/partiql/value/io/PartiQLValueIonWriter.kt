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
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
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

        private inline fun <T : PartiQLValue> T.annotate(block: T.() -> IonElement): IonElement {
            val e = this.block()
            return e.withAnnotations(this.annotations)
        }

        override fun visitNull(v: NullValue, ctx: Unit): IonElement = ionNull().withAnnotations(v.annotations)

        override fun visitMissing(v: MissingValue, ctx: Unit): IonElement = ionNull().withAnnotations(
            v.annotations + listOf(
                MISSING_ANNOTATION
            )
        )

        override fun visitBool(v: BoolValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.BOOL)
                else -> ionBool(value)
            }
        }

        override fun visitInt8(v: Int8Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.INT)
                else -> ionInt(value.toLong())
            }
        }

        override fun visitInt16(v: Int16Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.INT)
                else -> ionInt(value.toLong())
            }
        }

        override fun visitInt32(v: Int32Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.INT)
                else -> ionInt(value.toLong())
            }
        }

        override fun visitInt64(v: Int64Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.INT)
                else -> ionInt(value)
            }
        }

        override fun visitInt(v: IntValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.INT)
                else -> ionInt(value)
            }
        }

        override fun visitDecimal(v: DecimalValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.DECIMAL)
                else -> ionDecimal(Decimal.valueOf(value))
            }
        }

        // TODO : This is wrong. Ion float is 64 bit, when we cast the 32 bit float to double, we deliver false promise
        override fun visitFloat32(v: Float32Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.FLOAT)
                else -> ionFloat(value.toString().toDouble())
            }
        }

        override fun visitFloat64(v: Float64Value, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.FLOAT)
                else -> ionFloat(value.toString().toDouble())
            }
        }

        // TODO: Revisit
        override fun visitChar(v: CharValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.STRING)
                else -> ionString(value.toString())
            }
        }

        override fun visitString(v: StringValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.STRING)
                else -> ionString(value)
            }
        }

        override fun visitSymbol(v: SymbolValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.SYMBOL)
                else -> ionSymbol(value)
            }
        }

        override fun visitClob(v: ClobValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.CLOB)
                else -> ionClob(value)
            }
        }

        // TODO: Revisit
        override fun visitBinary(v: BinaryValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.BLOB)
                else -> ionBlob(value.toByteArray())
            }
        }

        // TODO: Revisit
        override fun visitByte(v: ByteValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.BLOB)
                else -> ionBlob(ByteArray(1) { value })
            }
        }

        override fun visitBlob(v: BlobValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.BLOB)
                else -> ionBlob(value)
            }
        }

        override fun visitDate(v: DateValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.STRUCT)
                else -> {
                    ionStructOf(
                        field("year", ionInt(value.year.toLong())),
                        field("month", ionInt(value.month.toLong())),
                        field("day", ionInt(value.day.toLong()))
                    )
                }
            }
        }.withAnnotations(DATE_ANNOTATION)

        override fun visitTime(v: TimeValue, ctx: Unit): IonElement = v.annotate {
            when (val value = v.value) {
                null -> ionNull(ElementType.STRUCT)
                else -> {
                    when (val timeZone = value.timeZone) {
                        TimeZone.UnknownTimeZone ->
                            ionStructOf(
                                field("hour", ionInt(value.hour.toLong())),
                                field("minute", ionInt(value.minute.toLong())),
                                field("second", ionDecimal(Decimal.valueOf(value.decimalSecond))),
                                field("offset", ionNull(ElementType.INT)),
                            )
                        is TimeZone.UtcOffset ->
                            ionStructOf(
                                field("hour", ionInt(value.hour.toLong())),
                                field("minute", ionInt(value.minute.toLong())),
                                field("second", ionDecimal(Decimal.valueOf(value.decimalSecond))),
                                field("offset", ionInt(timeZone.totalOffsetMinutes.toLong()))
                            )
                        null ->
                            ionStructOf(
                                field("hour", ionInt(value.hour.toLong())),
                                field("minute", ionInt(value.minute.toLong())),
                                field("second", ionDecimal(Decimal.valueOf(value.decimalSecond)))
                            )
                    }
                }
            }
        }.withAnnotations(TIME_ANNOTATION)

        override fun visitTimestamp(v: TimestampValue, ctx: Unit): IonElement {
            return when (val timestamp = v.value) {
                // TODO: we actually don't know if this is a timestamp with timezone or timestamp without timezone
                //  Should we care?
                null -> v.annotate { ionNull(ElementType.TIMESTAMP) }
                is TimestampWithTimeZone -> v.annotate { ionTimestamp(timestamp.ionTimestampValue) }
                is TimestampWithoutTimeZone -> v.annotate {
                    ionStructOf(
                        field("year", ionInt(timestamp.year.toLong())),
                        field("month", ionInt(timestamp.month.toLong())),
                        field("day", ionInt(timestamp.day.toLong())),
                        field("hour", ionInt(timestamp.hour.toLong())),
                        field("minute", ionInt(timestamp.minute.toLong())),
                        field("second", ionDecimal(Decimal.valueOf(timestamp.decimalSecond)))
                    )
                }.withAnnotations(TIMESTAMP_ANNOTATION)
            }
        }

        override fun visitInterval(v: IntervalValue, ctx: Unit): IonElement = TODO("Not Yet supported")

        override fun visitBag(v: BagValue<*>, ctx: Unit): IonElement = v.annotate {
            when (val elements = v.elements) {
                null -> ionNull(ElementType.LIST)
                else -> ionListOf(elements.map { it.accept(ToIon, Unit) }.toList())
            }
        }.withAnnotations(BAG_ANNOTATION)

        override fun visitList(v: ListValue<*>, ctx: Unit): IonElement = v.annotate {
            when (val elements = v.elements) {
                null -> ionNull(ElementType.LIST)
                else -> ionListOf(elements.map { it.accept(ToIon, Unit) }.toList())
            }
        }

        override fun visitSexp(v: SexpValue<*>, ctx: Unit): IonElement = v.annotate {
            when (val elements = v.elements) {
                null -> ionNull(ElementType.SEXP)
                else -> ionSexpOf(elements.map { it.accept(ToIon, Unit) }.toList())
            }
        }

        override fun visitStruct(v: StructValue<*>, ctx: Unit): IonElement = v.annotate {
            when (val fields = v.fields) {
                null -> ionNull(ElementType.STRUCT)
                else -> {
                    val ionFields = fields.map {
                        val fk = it.first
                        val fv = it.second.accept(ToIon, ctx)
                        field(fk, fv)
                    }.toList()
                    ionStructOf(ionFields)
                }
            }
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
