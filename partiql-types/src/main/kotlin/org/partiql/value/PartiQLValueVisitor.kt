package org.partiql.value

public interface PartiQLValueVisitor<R, C> {

    public fun visit(v: PartiQLValue, ctx: C): R

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

    public fun visitNull(v: NullValue, ctx: C): R

    public fun visitMissing(v: MissingValue, ctx: C): R

    public fun visitNullableScalar(v: NullableScalarValue<*>, ctx: C): R

    public fun visitNullableCollection(v: NullableCollectionValue<*>, ctx: C): R

    public fun visitNullableBool(v: NullableBoolValue, ctx: C): R

    public fun visitNullableNumeric(v: NullableNumericValue<*>, ctx: C): R

    public fun visitNullableInt8(v: NullableInt8Value, ctx: C): R

    public fun visitNullableInt16(v: NullableInt16Value, ctx: C): R

    public fun visitNullableInt32(v: NullableInt32Value, ctx: C): R

    public fun visitNullableInt64(v: NullableInt64Value, ctx: C): R

    public fun visitNullableInt(v: NullableIntValue, ctx: C): R

    public fun visitNullableDecimal(v: NullableDecimalValue, ctx: C): R

    public fun visitNullableFloat32(v: NullableFloat32Value, ctx: C): R

    public fun visitNullableFloat64(v: NullableFloat64Value, ctx: C): R

    public fun visitNullableText(v: NullableTextValue<*>, ctx: C): R

    public fun visitNullableChar(v: NullableCharValue, ctx: C): R

    public fun visitNullableString(v: NullableStringValue, ctx: C): R

    public fun visitNullableSymbol(v: NullableSymbolValue, ctx: C): R

    public fun visitNullableClob(v: NullableClobValue, ctx: C): R

    public fun visitNullableBinary(v: NullableBinaryValue, ctx: C): R

    public fun visitNullableByte(v: NullableByteValue, ctx: C): R

    public fun visitNullableBlob(v: NullableBlobValue, ctx: C): R

    public fun visitNullableDate(v: NullableDateValue, ctx: C): R

    public fun visitNullableTime(v: NullableTimeValue, ctx: C): R

    public fun visitNullableTimestamp(v: NullableTimestampValue, ctx: C): R

    public fun visitNullableInterval(v: NullableIntervalValue, ctx: C): R

    public fun visitNullableBag(v: NullableBagValue<*>, ctx: C): R

    public fun visitNullableList(v: NullableListValue<*>, ctx: C): R

    public fun visitNullableSexp(v: NullableSexpValue<*>, ctx: C): R

    public fun visitNullableStruct(v: NullableStructValue<*>, ctx: C): R
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
            is NullableCollectionValue<*> -> {
                v.elements?.forEach { it.accept(this, ctx) }
            }
            is NullableStructValue<*> -> {
                v.fields?.forEach { it.second.accept(this, ctx) }
            }
            else -> {}
        }
        return defaultReturn(v, ctx)
    }

    public abstract fun defaultReturn(v: PartiQLValue, ctx: C): R

    override fun visit(v: PartiQLValue, ctx: C): R = v.accept(this, ctx)

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

    override fun visitNull(v: NullValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitMissing(v: MissingValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableScalar(v: NullableScalarValue<*>, ctx: C): R = when (v) {
        is NullableBinaryValue -> visitNullableBinary(v, ctx)
        is NullableBlobValue -> visitNullableBlob(v, ctx)
        is NullableBoolValue -> visitNullableBool(v, ctx)
        is NullableByteValue -> visitNullableByte(v, ctx)
        is NullableDateValue -> visitNullableDate(v, ctx)
        is NullableFloat32Value -> visitNullableFloat32(v, ctx)
        is NullableFloat64Value -> visitNullableFloat64(v, ctx)
        is NullableIntervalValue -> visitNullableInterval(v, ctx)
        is NullableDecimalValue -> visitNullableDecimal(v, ctx)
        is NullableInt16Value -> visitNullableInt16(v, ctx)
        is NullableInt32Value -> visitNullableInt32(v, ctx)
        is NullableInt64Value -> visitNullableInt64(v, ctx)
        is NullableInt8Value -> visitNullableInt8(v, ctx)
        is NullableIntValue -> visitNullableInt(v, ctx)
        is NullableCharValue -> visitNullableChar(v, ctx)
        is NullableClobValue -> visitNullableClob(v, ctx)
        is NullableStringValue -> visitNullableString(v, ctx)
        is NullableSymbolValue -> visitNullableSymbol(v, ctx)
        is NullableTimeValue -> visitNullableTime(v, ctx)
        is NullableTimestampValue -> visitNullableTimestamp(v, ctx)
    }

    override fun visitNullableCollection(v: NullableCollectionValue<*>, ctx: C): R = when (v) {
        is NullableBagValue -> visitNullableBag(v, ctx)
        is NullableListValue -> visitNullableList(v, ctx)
        is NullableSexpValue -> visitNullableSexp(v, ctx)
    }

    override fun visitNullableBool(v: NullableBoolValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableNumeric(v: NullableNumericValue<*>, ctx: C): R = when (v) {
        is NullableDecimalValue -> visitNullableDecimal(v, ctx)
        is NullableInt16Value -> visitNullableInt16(v, ctx)
        is NullableInt32Value -> visitNullableInt32(v, ctx)
        is NullableInt64Value -> visitNullableInt64(v, ctx)
        is NullableInt8Value -> visitNullableInt8(v, ctx)
        is NullableIntValue -> visitNullableInt(v, ctx)
    }

    override fun visitNullableInt8(v: NullableInt8Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableInt16(v: NullableInt16Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableInt32(v: NullableInt32Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableInt64(v: NullableInt64Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableInt(v: NullableIntValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableDecimal(v: NullableDecimalValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableFloat32(v: NullableFloat32Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableFloat64(v: NullableFloat64Value, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableText(v: NullableTextValue<*>, ctx: C): R = when (v) {
        is NullableCharValue -> visitNullableChar(v, ctx)
        is NullableClobValue -> visitNullableClob(v, ctx)
        is NullableStringValue -> visitNullableString(v, ctx)
        is NullableSymbolValue -> visitNullableSymbol(v, ctx)
    }

    override fun visitNullableChar(v: NullableCharValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableString(v: NullableStringValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableSymbol(v: NullableSymbolValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableClob(v: NullableClobValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableBinary(v: NullableBinaryValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableByte(v: NullableByteValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableBlob(v: NullableBlobValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableDate(v: NullableDateValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableTime(v: NullableTimeValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableTimestamp(v: NullableTimestampValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableInterval(v: NullableIntervalValue, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableBag(v: NullableBagValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableList(v: NullableListValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableSexp(v: NullableSexpValue<*>, ctx: C): R = defaultVisit(v, ctx)

    override fun visitNullableStruct(v: NullableStructValue<*>, ctx: C): R = defaultVisit(v, ctx)
}
