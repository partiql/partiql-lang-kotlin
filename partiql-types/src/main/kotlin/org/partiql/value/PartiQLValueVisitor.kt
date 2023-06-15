package org.partiql.value

public interface PartiQLValueVisitor<R, C> {

    public fun visit(v: PartiQLValue, ctx: C): R

    public fun visitNull(v: NullValue, ctx: C): R

    public fun visitMissing(v: MissingValue, ctx: C): R

    public fun visitScalar(v: ScalarValue<*>, ctx: C): R

    public fun visitCollection(v: CollectionValue<*>, ctx: C): R

    public fun visitBool(v: BoolValue, ctx: C): R

    public fun visitNumeric(v: NumericValue<*>, ctx: C): R

    public fun visitInt8(v: Int8Value, ctx: C): R

    public fun visitInt16(v: Int16Value, ctx: C): R

    public fun visitInt32(v: Int32Value, ctx: C): R

    public fun visitInt64(v: Int64Value, ctx: C): R

    public fun visitInt(v: IntValue, ctx: C): R

    public fun visitDecimal(v: DecimalValue, ctx: C): R

    public fun visitFloat32(v: Float32Value, ctx: C): R

    public fun visitFloat64(v: Float64Value, ctx: C): R

    public fun visitText(v: TextValue<*>, ctx: C): R

    public fun visitChar(v: CharValue, ctx: C): R

    public fun visitString(v: StringValue, ctx: C): R

    public fun visitSymbol(v: SymbolValue, ctx: C): R

    public fun visitClob(v: ClobValue, ctx: C): R

    public fun visitBinary(v: BinaryValue, ctx: C): R

    public fun visitByte(v: ByteValue, ctx: C): R

    public fun visitBlob(v: BlobValue, ctx: C): R

    public fun visitDate(v: DateValue, ctx: C): R

    public fun visitTime(v: TimeValue, ctx: C): R

    public fun visitTimestamp(v: TimestampValue, ctx: C): R

    public fun visitInterval(v: IntervalValue, ctx: C): R

    public fun visitBag(v: BagValue<*>, ctx: C): R

    public fun visitList(v: ListValue<*>, ctx: C): R

    public fun visitSexp(v: SexpValue<*>, ctx: C): R

    public fun visitStruct(v: StructValue<*>, ctx: C): R

    public fun visitAny(v: AnyValue, ctx: C): R
}

public abstract class PartiQLValueBaseVisitor<R, C> : PartiQLValueVisitor<R, C> {

    public open fun defaultVisit(v: PartiQLValue, ctx: C): R {
        when (v) {
            is CollectionValue<*> -> {
                v.elements.forEach { it.accept(this, ctx) }
            }
            is StructValue<*> -> {
                v.fields.forEach { it.second.accept(this, ctx) }
            }
            else -> {}
        }
        return defaultReturn(v, ctx)
    }

    public abstract fun defaultReturn(v: PartiQLValue, ctx: C): R

    override fun visit(v: PartiQLValue, ctx: C): R = v.accept(this, ctx)

    override fun visitNull(v: NullValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitMissing(v: MissingValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitScalar(v: ScalarValue<*>, ctx: C): R = when (v) {
        is BinaryValue -> visitBinary(v, ctx)
        is BlobValue -> visitBlob(v, ctx)
        is BoolValue -> visitBool(v, ctx)
        is ByteValue -> visitByte(v, ctx)
        is DateValue -> visitDate(v, ctx)
        is Float32Value -> visitFloat32(v, ctx)
        is Float64Value -> visitFloat64(v, ctx)
        is IntervalValue -> visitInterval(v, ctx)
        is DecimalValue -> visitDecimal(v, ctx)
        is Int16Value -> visitInt16(v, ctx)
        is Int32Value -> visitInt32(v, ctx)
        is Int64Value -> visitInt64(v, ctx)
        is Int8Value -> visitInt8(v, ctx)
        is IntValue -> visitInt(v, ctx)
        is CharValue -> visitChar(v, ctx)
        is ClobValue -> visitClob(v, ctx)
        is StringValue -> visitString(v, ctx)
        is SymbolValue -> visitSymbol(v, ctx)
        is TimeValue -> visitTime(v, ctx)
        is TimestampValue -> visitTimestamp(v, ctx)
    }

    override fun visitCollection(v: CollectionValue<*>, ctx: C): R = when (v) {
        is BagValue -> visitBag(v, ctx)
        is ListValue -> visitList(v, ctx)
        is SexpValue -> visitSexp(v, ctx)
    }

    override fun visitBool(v: BoolValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNumeric(v: NumericValue<*>, ctx: C): R = when (v) {
        is DecimalValue -> visitDecimal(v, ctx)
        is Int16Value -> visitInt16(v, ctx)
        is Int32Value -> visitInt32(v, ctx)
        is Int64Value -> visitInt64(v, ctx)
        is Int8Value -> visitInt8(v, ctx)
        is IntValue -> visitInt(v, ctx)
    }

    override fun visitInt8(v: Int8Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitInt16(v: Int16Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitInt32(v: Int32Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitInt64(v: Int64Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitInt(v: IntValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitDecimal(v: DecimalValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitFloat32(v: Float32Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitFloat64(v: Float64Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitText(v: TextValue<*>, ctx: C): R = when (v) {
        is CharValue -> visitChar(v, ctx)
        is ClobValue -> visitClob(v, ctx)
        is StringValue -> visitString(v, ctx)
        is SymbolValue -> visitSymbol(v, ctx)
    }

    override fun visitChar(v: CharValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitString(v: StringValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitSymbol(v: SymbolValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitClob(v: ClobValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitBinary(v: BinaryValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitByte(v: ByteValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitBlob(v: BlobValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitDate(v: DateValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitTime(v: TimeValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitTimestamp(v: TimestampValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitInterval(v: IntervalValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitBag(v: BagValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitList(v: ListValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitSexp(v: SexpValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitStruct(v: StructValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitAny(v: AnyValue, ctx: C): R = v.value.accept(this, ctx)
}
