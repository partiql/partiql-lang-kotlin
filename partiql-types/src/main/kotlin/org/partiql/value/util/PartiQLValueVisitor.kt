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
