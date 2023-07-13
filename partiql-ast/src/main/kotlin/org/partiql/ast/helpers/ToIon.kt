package org.partiql.ast.helpers

import com.amazon.ion.Decimal
import com.amazon.ion.Timestamp
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
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.ClobValue
import org.partiql.value.CollectionValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.ScalarValue
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.util.PartiQLValueBaseVisitor

/**
 * PartiQL Value .toIon helper; to be replaced by https://github.com/partiql/partiql-lang-kotlin/pull/1131/files
 *
 * TODO add `lower` mode, this just errors
 */
@OptIn(PartiQLValueExperimental::class)
internal object ToIon : PartiQLValueBaseVisitor<IonElement, Unit>() {

    private inline fun <T> ScalarValue<T>.toIon(block: ScalarValue<T>.() -> IonElement): IonElement {
        val e = this.block()
        return e.withAnnotations(this.annotations)
    }

    private inline fun CollectionValue<*>.toIon(block: CollectionValue<*>.(elements: List<IonElement>) -> IonElement): IonElement {
        val elements = this.elements.map { it.accept(ToIon, Unit) }
        val e = this.block(elements)
        return e.withAnnotations(this.annotations)
    }

    override fun defaultVisit(v: PartiQLValue, ctx: Unit) = defaultReturn(v, ctx)

    override fun defaultReturn(v: PartiQLValue, ctx: Unit) =
        throw IllegalArgumentException("Cannot represent $v as Ion in strict mode")

    override fun visitNull(v: NullValue, ctx: Unit) = ionNull().withAnnotations(v.annotations)

    override fun visitBool(v: BoolValue, ctx: Unit) = v.toIon { ionBool(value) }

    override fun visitInt8(v: Int8Value, ctx: Unit) = v.toIon { ionInt(value.toLong()) }

    override fun visitInt16(v: Int16Value, ctx: Unit) = v.toIon { ionInt(value.toLong()) }

    override fun visitInt32(v: Int32Value, ctx: Unit) = v.toIon { ionInt(value.toLong()) }

    override fun visitInt64(v: Int64Value, ctx: Unit) = v.toIon { ionInt(value) }

    // Call .toLong() because IonElement .equals() is failing with BigInteger (it's comparing by reference).
    override fun visitInt(v: IntValue, ctx: Unit) = v.toIon { ionInt(value.toLong()) }

    override fun visitDecimal(v: DecimalValue, ctx: Unit) = v.toIon { ionDecimal(Decimal.valueOf(value)) }

    override fun visitFloat32(v: Float32Value, ctx: Unit) = v.toIon { ionFloat(value.toString().toDouble()) }

    override fun visitFloat64(v: Float64Value, ctx: Unit) = v.toIon { ionFloat(value) }

    override fun visitString(v: StringValue, ctx: Unit) = v.toIon { ionString(value) }

    override fun visitSymbol(v: SymbolValue, ctx: Unit) = v.toIon { ionSymbol(value) }

    override fun visitClob(v: ClobValue, ctx: Unit) = v.toIon { ionClob(value) }

    override fun visitBlob(v: BlobValue, ctx: Unit) = v.toIon { ionBlob(value) }

    override fun visitTimestamp(v: TimestampValue, ctx: Unit) = v.toIon {
        val offset = when (val z = v.value.timeZone) {
            TimeZone.UnknownTimeZone -> null
            is TimeZone.UtcOffset -> z.totalOffsetMinutes
            null -> 0
        }
        val timestamp = Timestamp.forSecond(
            v.value.year,
            v.value.month,
            v.value.day,
            v.value.hour,
            v.value.minute,
            v.value.decimalSecond,
            offset,
        )
        ionTimestamp(timestamp)
    }

    override fun visitList(v: ListValue<*>, ctx: Unit) = v.toIon { elements -> ionListOf(elements) }

    override fun visitSexp(v: SexpValue<*>, ctx: Unit) = v.toIon { elements -> ionSexpOf(elements) }

    override fun visitStruct(v: StructValue<*>, ctx: Unit): IonElement {
        val fields = v.fields.map {
            val key = it.first
            val value = it.second.accept(this, ctx)
            field(key, value)
        }
        return ionStructOf(fields, v.annotations)
    }
}
