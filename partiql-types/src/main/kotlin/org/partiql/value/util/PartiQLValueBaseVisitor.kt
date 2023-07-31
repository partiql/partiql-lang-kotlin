/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.util

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
import org.partiql.value.NullableCollectionValue
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
import org.partiql.value.NullableNumericValue
import org.partiql.value.NullableScalarValue
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableStructValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.NullableTextValue
import org.partiql.value.NullableTimeValue
import org.partiql.value.NullableTimestampValue
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.ScalarValue
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TextValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue

@PartiQLValueExperimental
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
                if (!v.isNull()) {
                    v.promote().elements.forEach { it.accept(this, ctx) }
                }
            }
            is NullableStructValue<*> -> {
                if (!v.isNull()) {
                    v.promote().fields.forEach { it.second.accept(this, ctx) }
                }
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
        is Float32Value -> visitFloat32(v, ctx)
        is Float64Value -> visitFloat64(v, ctx)
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
