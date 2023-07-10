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

package org.partiql.value

import org.partiql.types.PartiQLValueType
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.BitSet

internal typealias Annotations = List<String>

/**
 * TODO
 *  - Implement value equality for comparisons of nullable and non-nullable value classes
 *  - Relate types T and UNION(NULL, T) with an abstract base
 *  - Implement comparators
 */
@PartiQLValueExperimental
public sealed interface PartiQLValue {

    public val type: PartiQLValueType

    public val annotations: Annotations

    public fun copy(annotations: Annotations = this.annotations): PartiQLValue

    public fun withAnnotations(annotations: Annotations): PartiQLValue

    public fun withoutAnnotations(): PartiQLValue

    public fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R
}

@PartiQLValueExperimental
public abstract class MissingValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.MISSING

    abstract override fun copy(annotations: Annotations): MissingValue

    abstract override fun withAnnotations(annotations: Annotations): MissingValue

    abstract override fun withoutAnnotations(): MissingValue
}

@PartiQLValueExperimental
public sealed interface ScalarValue<T> : PartiQLValue {

    public val value: T

    override fun copy(annotations: Annotations): ScalarValue<T>

    override fun withAnnotations(annotations: Annotations): ScalarValue<T>

    override fun withoutAnnotations(): ScalarValue<T>
}

@PartiQLValueExperimental
public sealed interface CollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val elements: Collection<T>

    override fun copy(annotations: Annotations): CollectionValue<T>

    override fun withAnnotations(annotations: Annotations): CollectionValue<T>

    override fun withoutAnnotations(): CollectionValue<T>
}

@PartiQLValueExperimental
public abstract class BoolValue : ScalarValue<Boolean> {

    override val type: PartiQLValueType = PartiQLValueType.BOOL

    abstract override fun copy(annotations: Annotations): BoolValue

    abstract override fun withAnnotations(annotations: Annotations): BoolValue

    abstract override fun withoutAnnotations(): BoolValue
}

@PartiQLValueExperimental
public sealed class NumericValue<T : Number> : ScalarValue<T> {

    public val int: Int
        get() = value.toInt()

    public val long: Long
        get() = value.toLong()

    public val float: Float
        get() = value.toFloat()

    public val double: Double
        get() = value.toDouble()

    abstract override fun copy(annotations: Annotations): NumericValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NumericValue<T>

    abstract override fun withoutAnnotations(): NumericValue<T>
}

@PartiQLValueExperimental
public abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLValueType = PartiQLValueType.INT8

    abstract override fun copy(annotations: Annotations): Int8Value

    abstract override fun withAnnotations(annotations: Annotations): Int8Value

    abstract override fun withoutAnnotations(): Int8Value
}

@PartiQLValueExperimental
public abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLValueType = PartiQLValueType.INT16

    abstract override fun copy(annotations: Annotations): Int16Value

    abstract override fun withAnnotations(annotations: Annotations): Int16Value

    abstract override fun withoutAnnotations(): Int16Value
}

@PartiQLValueExperimental
public abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLValueType = PartiQLValueType.INT32

    abstract override fun copy(annotations: Annotations): Int32Value

    abstract override fun withAnnotations(annotations: Annotations): Int32Value

    abstract override fun withoutAnnotations(): Int32Value
}

@PartiQLValueExperimental
public abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLValueType = PartiQLValueType.INT64

    abstract override fun copy(annotations: Annotations): Int64Value

    abstract override fun withAnnotations(annotations: Annotations): Int64Value

    abstract override fun withoutAnnotations(): Int64Value
}

@PartiQLValueExperimental
public abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLValueType = PartiQLValueType.INT

    abstract override fun copy(annotations: Annotations): IntValue

    abstract override fun withAnnotations(annotations: Annotations): IntValue

    abstract override fun withoutAnnotations(): IntValue
}

@PartiQLValueExperimental
public abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLValueType = PartiQLValueType.DECIMAL

    abstract override fun copy(annotations: Annotations): DecimalValue

    abstract override fun withAnnotations(annotations: Annotations): DecimalValue

    abstract override fun withoutAnnotations(): DecimalValue
}

@PartiQLValueExperimental
public abstract class Float32Value : ScalarValue<Float> {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT32

    abstract override fun copy(annotations: Annotations): Float32Value

    abstract override fun withAnnotations(annotations: Annotations): Float32Value

    abstract override fun withoutAnnotations(): Float32Value
}

@PartiQLValueExperimental
public abstract class Float64Value : ScalarValue<Double> {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT64

    abstract override fun copy(annotations: Annotations): Float64Value

    abstract override fun withAnnotations(annotations: Annotations): Float64Value

    abstract override fun withoutAnnotations(): Float64Value
}

@PartiQLValueExperimental
public sealed class TextValue<T> : ScalarValue<T> {

    public abstract val string: String

    abstract override fun copy(annotations: Annotations): TextValue<T>

    abstract override fun withAnnotations(annotations: Annotations): TextValue<T>

    abstract override fun withoutAnnotations(): TextValue<T>
}

@PartiQLValueExperimental
public abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLValueType = PartiQLValueType.CHAR

    override val string: String
        get() = value.toString()

    abstract override fun copy(annotations: Annotations): CharValue

    abstract override fun withAnnotations(annotations: Annotations): CharValue

    abstract override fun withoutAnnotations(): CharValue
}

@PartiQLValueExperimental
public abstract class StringValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.STRING

    override val string: String
        get() = value

    abstract override fun copy(annotations: Annotations): StringValue

    abstract override fun withAnnotations(annotations: Annotations): StringValue

    abstract override fun withoutAnnotations(): StringValue
}

@PartiQLValueExperimental
public abstract class SymbolValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.SYMBOL

    override val string: String
        get() = value

    abstract override fun copy(annotations: Annotations): SymbolValue

    abstract override fun withAnnotations(annotations: Annotations): SymbolValue

    abstract override fun withoutAnnotations(): SymbolValue
}

@PartiQLValueExperimental
public abstract class ClobValue : TextValue<ByteArray>() {

    override val type: PartiQLValueType = PartiQLValueType.CLOB

    override val string: String
        get() = value.toString(Charsets.UTF_8)

    abstract override fun copy(annotations: Annotations): ClobValue

    abstract override fun withAnnotations(annotations: Annotations): ClobValue

    abstract override fun withoutAnnotations(): ClobValue
}

@PartiQLValueExperimental
public abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLValueType = PartiQLValueType.BINARY

    abstract override fun copy(annotations: Annotations): BinaryValue

    abstract override fun withAnnotations(annotations: Annotations): BinaryValue

    abstract override fun withoutAnnotations(): BinaryValue
}

@PartiQLValueExperimental
public abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLValueType = PartiQLValueType.BYTE

    abstract override fun copy(annotations: Annotations): ByteValue

    abstract override fun withAnnotations(annotations: Annotations): ByteValue

    abstract override fun withoutAnnotations(): ByteValue
}

@PartiQLValueExperimental
public abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLValueType = PartiQLValueType.BLOB

    abstract override fun copy(annotations: Annotations): BlobValue

    abstract override fun withAnnotations(annotations: Annotations): BlobValue

    abstract override fun withoutAnnotations(): BlobValue
}

@PartiQLValueExperimental
public abstract class DateValue : ScalarValue<LocalDate> {

    override val type: PartiQLValueType = PartiQLValueType.DATE

    abstract override fun copy(annotations: Annotations): DateValue

    abstract override fun withAnnotations(annotations: Annotations): DateValue

    abstract override fun withoutAnnotations(): DateValue
}

@PartiQLValueExperimental
public abstract class TimeValue : ScalarValue<LocalTime> {

    override val type: PartiQLValueType = PartiQLValueType.TIME

    // TEMPORARY
    public abstract val precision: Int

    // TEMPORARY
    public abstract val offset: ZoneOffset?

    // TEMPORARY
    public abstract val withZone: Boolean

    abstract override fun copy(annotations: Annotations): TimeValue

    abstract override fun withAnnotations(annotations: Annotations): TimeValue

    abstract override fun withoutAnnotations(): TimeValue
}

@PartiQLValueExperimental
public abstract class TimestampValue : ScalarValue<LocalDateTime> {

    override val type: PartiQLValueType = PartiQLValueType.TIMESTAMP

    // TEMPORARY
    public abstract val precision: Int

    // TEMPORARY
    public abstract val offset: ZoneOffset?

    // TEMPORARY
    public abstract val withZone: Boolean

    abstract override fun copy(annotations: Annotations): TimestampValue

    abstract override fun withAnnotations(annotations: Annotations): TimestampValue

    abstract override fun withoutAnnotations(): TimestampValue
}

@PartiQLValueExperimental
public abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.INTERVAL

    abstract override fun copy(annotations: Annotations): IntervalValue

    abstract override fun withAnnotations(annotations: Annotations): IntervalValue

    abstract override fun withoutAnnotations(): IntervalValue
}

@PartiQLValueExperimental
public abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.BAG

    abstract override fun copy(annotations: Annotations): BagValue<T>

    abstract override fun withAnnotations(annotations: Annotations): BagValue<T>

    abstract override fun withoutAnnotations(): BagValue<T>
}

@PartiQLValueExperimental
public abstract class ListValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.LIST

    abstract override fun copy(annotations: Annotations): ListValue<T>

    abstract override fun withAnnotations(annotations: Annotations): ListValue<T>

    abstract override fun withoutAnnotations(): ListValue<T>
}

@PartiQLValueExperimental
public abstract class SexpValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.SEXP

    abstract override fun copy(annotations: Annotations): SexpValue<T>

    abstract override fun withAnnotations(annotations: Annotations): SexpValue<T>

    abstract override fun withoutAnnotations(): SexpValue<T>
}

@PartiQLValueExperimental
public abstract class StructValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    public abstract val fields: List<Pair<String, T>>

    override val type: PartiQLValueType = PartiQLValueType.STRUCT

    abstract override fun copy(annotations: Annotations): StructValue<T>

    abstract override fun withAnnotations(annotations: Annotations): StructValue<T>

    abstract override fun withoutAnnotations(): StructValue<T>
}

@PartiQLValueExperimental
public abstract class NullValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.NULL

    abstract override fun copy(annotations: Annotations): NullValue

    abstract override fun withAnnotations(annotations: Annotations): NullValue

    abstract override fun withoutAnnotations(): NullValue
}

@PartiQLValueExperimental
public sealed interface NullableScalarValue<T> : PartiQLValue {

    public val value: T?

    override fun copy(annotations: Annotations): NullableScalarValue<T>

    override fun withAnnotations(annotations: Annotations): NullableScalarValue<T>

    override fun withoutAnnotations(): NullableScalarValue<T>
}

@PartiQLValueExperimental
public sealed interface NullableCollectionValue<T : PartiQLValue> : PartiQLValue {
    public fun isNull(): Boolean

    public fun promote(): CollectionValue<T>

    override fun copy(annotations: Annotations): NullableCollectionValue<T>

    override fun withAnnotations(annotations: Annotations): NullableCollectionValue<T>

    override fun withoutAnnotations(): NullableCollectionValue<T>
}

@PartiQLValueExperimental
public abstract class NullableBoolValue : NullableScalarValue<Boolean> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BOOL

    abstract override fun copy(annotations: Annotations): NullableBoolValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBoolValue

    abstract override fun withoutAnnotations(): NullableBoolValue
}

@PartiQLValueExperimental
public sealed class NullableNumericValue<T : Number> : NullableScalarValue<T> {

    public val int: Int?
        get() = value?.toInt()

    public val long: Long?
        get() = value?.toLong()

    public val float: Float?
        get() = value?.toFloat()

    public val double: Double?
        get() = value?.toDouble()

    abstract override fun copy(annotations: Annotations): NullableNumericValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableNumericValue<T>

    abstract override fun withoutAnnotations(): NullableNumericValue<T>
}

@PartiQLValueExperimental
public abstract class NullableInt8Value : NullableNumericValue<Byte>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT8

    abstract override fun copy(annotations: Annotations): NullableInt8Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt8Value

    abstract override fun withoutAnnotations(): NullableInt8Value
}

@PartiQLValueExperimental
public abstract class NullableInt16Value : NullableNumericValue<Short>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT16

    abstract override fun copy(annotations: Annotations): NullableInt16Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt16Value

    abstract override fun withoutAnnotations(): NullableInt16Value
}

@PartiQLValueExperimental
public abstract class NullableInt32Value : NullableNumericValue<Int>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT32

    abstract override fun copy(annotations: Annotations): NullableInt32Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt32Value

    abstract override fun withoutAnnotations(): NullableInt32Value
}

@PartiQLValueExperimental
public abstract class NullableInt64Value : NullableNumericValue<Long>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT64

    abstract override fun copy(annotations: Annotations): NullableInt64Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt64Value

    abstract override fun withoutAnnotations(): NullableInt64Value
}

@PartiQLValueExperimental
public abstract class NullableIntValue : NullableNumericValue<BigInteger>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT

    abstract override fun copy(annotations: Annotations): NullableIntValue

    abstract override fun withAnnotations(annotations: Annotations): NullableIntValue

    abstract override fun withoutAnnotations(): NullableIntValue
}

@PartiQLValueExperimental
public abstract class NullableDecimalValue : NullableNumericValue<BigDecimal>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_DECIMAL

    abstract override fun copy(annotations: Annotations): NullableDecimalValue

    abstract override fun withAnnotations(annotations: Annotations): NullableDecimalValue

    abstract override fun withoutAnnotations(): NullableDecimalValue
}

@PartiQLValueExperimental
public abstract class NullableFloat32Value : NullableScalarValue<Float> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_FLOAT32

    abstract override fun copy(annotations: Annotations): NullableFloat32Value

    abstract override fun withAnnotations(annotations: Annotations): NullableFloat32Value

    abstract override fun withoutAnnotations(): NullableFloat32Value
}

@PartiQLValueExperimental
public abstract class NullableFloat64Value : NullableScalarValue<Double> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_FLOAT64

    abstract override fun copy(annotations: Annotations): NullableFloat64Value

    abstract override fun withAnnotations(annotations: Annotations): NullableFloat64Value

    abstract override fun withoutAnnotations(): NullableFloat64Value
}

@PartiQLValueExperimental
public sealed class NullableTextValue<T> : NullableScalarValue<T> {

    public abstract val string: String?

    abstract override fun copy(annotations: Annotations): NullableTextValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableTextValue<T>

    abstract override fun withoutAnnotations(): NullableTextValue<T>
}

@PartiQLValueExperimental
public abstract class NullableCharValue : NullableTextValue<Char>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_CHAR

    override val string: String?
        get() = value?.toString()

    abstract override fun copy(annotations: Annotations): NullableCharValue

    abstract override fun withAnnotations(annotations: Annotations): NullableCharValue

    abstract override fun withoutAnnotations(): NullableCharValue
}

@PartiQLValueExperimental
public abstract class NullableStringValue : NullableTextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_STRING

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): NullableStringValue

    abstract override fun withAnnotations(annotations: Annotations): NullableStringValue

    abstract override fun withoutAnnotations(): NullableStringValue
}

@PartiQLValueExperimental
public abstract class NullableSymbolValue : NullableTextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_SYMBOL

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): NullableSymbolValue

    abstract override fun withAnnotations(annotations: Annotations): NullableSymbolValue

    abstract override fun withoutAnnotations(): NullableSymbolValue
}

@PartiQLValueExperimental
public abstract class NullableClobValue : NullableTextValue<ByteArray>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_CLOB

    override val string: String?
        get() = value?.toString(Charsets.UTF_8)

    abstract override fun copy(annotations: Annotations): NullableClobValue

    abstract override fun withAnnotations(annotations: Annotations): NullableClobValue

    abstract override fun withoutAnnotations(): NullableClobValue
}

@PartiQLValueExperimental
public abstract class NullableBinaryValue : NullableScalarValue<BitSet> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BINARY

    abstract override fun copy(annotations: Annotations): NullableBinaryValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBinaryValue

    abstract override fun withoutAnnotations(): NullableBinaryValue
}

@PartiQLValueExperimental
public abstract class NullableByteValue : NullableScalarValue<Byte> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BYTE

    abstract override fun copy(annotations: Annotations): NullableByteValue

    abstract override fun withAnnotations(annotations: Annotations): NullableByteValue

    abstract override fun withoutAnnotations(): NullableByteValue
}

@PartiQLValueExperimental
public abstract class NullableBlobValue : NullableScalarValue<ByteArray> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BLOB

    abstract override fun copy(annotations: Annotations): NullableBlobValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBlobValue

    abstract override fun withoutAnnotations(): NullableBlobValue
}

@PartiQLValueExperimental
public abstract class NullableDateValue : NullableScalarValue<LocalDate> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_DATE

    abstract override fun copy(annotations: Annotations): NullableDateValue

    abstract override fun withAnnotations(annotations: Annotations): NullableDateValue

    abstract override fun withoutAnnotations(): NullableDateValue
}

@PartiQLValueExperimental
public abstract class NullableTimeValue : NullableScalarValue<LocalTime> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_TIME

    // TEMPORARY
    public abstract val precision: Int

    // TEMPORARY
    public abstract val offset: ZoneOffset?

    // TEMPORARY
    public abstract val withZone: Boolean

    abstract override fun copy(annotations: Annotations): NullableTimeValue

    abstract override fun withAnnotations(annotations: Annotations): NullableTimeValue

    abstract override fun withoutAnnotations(): NullableTimeValue
}

@PartiQLValueExperimental
public abstract class NullableTimestampValue : NullableScalarValue<LocalDateTime> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_TIMESTAMP

    // TEMPORARY
    public abstract val precision: Int

    // TEMPORARY
    public abstract val offset: ZoneOffset?

    // TEMPORARY
    public abstract val withZone: Boolean

    abstract override fun copy(annotations: Annotations): NullableTimestampValue

    abstract override fun withAnnotations(annotations: Annotations): NullableTimestampValue

    abstract override fun withoutAnnotations(): NullableTimestampValue
}

@PartiQLValueExperimental
public abstract class NullableIntervalValue : NullableScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INTERVAL

    abstract override fun copy(annotations: Annotations): NullableIntervalValue

    abstract override fun withAnnotations(annotations: Annotations): NullableIntervalValue

    abstract override fun withoutAnnotations(): NullableIntervalValue
}

@PartiQLValueExperimental
public abstract class NullableBagValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BAG

    abstract override fun promote(): BagValue<T>

    abstract override fun copy(annotations: Annotations): NullableBagValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableBagValue<T>

    abstract override fun withoutAnnotations(): NullableBagValue<T>
}

@PartiQLValueExperimental
public abstract class NullableListValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_LIST

    abstract override fun promote(): ListValue<T>

    abstract override fun copy(annotations: Annotations): NullableListValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableListValue<T>

    abstract override fun withoutAnnotations(): NullableListValue<T>
}

@PartiQLValueExperimental
public abstract class NullableSexpValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_SEXP

    abstract override fun promote(): SexpValue<T>

    abstract override fun copy(annotations: Annotations): NullableSexpValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableSexpValue<T>

    abstract override fun withoutAnnotations(): NullableSexpValue<T>
}

@PartiQLValueExperimental
public abstract class NullableStructValue<T : PartiQLValue> : PartiQLValue {
    public abstract fun isNull(): Boolean

    public abstract fun promote(): StructValue<T>

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_STRUCT

    abstract override fun copy(annotations: Annotations): NullableStructValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableStructValue<T>

    abstract override fun withoutAnnotations(): NullableStructValue<T>
}
