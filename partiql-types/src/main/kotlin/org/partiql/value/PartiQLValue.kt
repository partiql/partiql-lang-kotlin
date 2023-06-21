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
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.BitSet
import java.util.Date

internal typealias Annotations = List<String>

public sealed interface PartiQLValue {

    public val type: PartiQLValueType

    public val annotations: Annotations

    public fun copy(annotations: Annotations = this.annotations): PartiQLValue

    public fun withAnnotations(annotations: Annotations): PartiQLValue

    public fun withoutAnnotations(): PartiQLValue

    public fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R
}

public abstract class NullValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.NULL

    abstract override fun copy(annotations: Annotations): NullValue

    abstract override fun withAnnotations(annotations: Annotations): NullValue

    abstract override fun withoutAnnotations(): NullValue
}

public abstract class MissingValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.MISSING

    abstract override fun copy(annotations: Annotations): MissingValue

    abstract override fun withAnnotations(annotations: Annotations): MissingValue

    abstract override fun withoutAnnotations(): MissingValue
}

public sealed interface ScalarValue<T> : PartiQLValue {

    public val value: T

    override fun copy(annotations: Annotations): ScalarValue<T>

    override fun withAnnotations(annotations: Annotations): ScalarValue<T>

    override fun withoutAnnotations(): ScalarValue<T>
}

public sealed interface CollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val elements: Collection<T>

    override fun copy(annotations: Annotations): CollectionValue<T>

    override fun withAnnotations(annotations: Annotations): CollectionValue<T>

    override fun withoutAnnotations(): CollectionValue<T>
}

public abstract class BoolValue : ScalarValue<Boolean> {

    override val type: PartiQLValueType = PartiQLValueType.BOOL

    abstract override fun copy(annotations: Annotations): BoolValue

    abstract override fun withAnnotations(annotations: Annotations): BoolValue

    abstract override fun withoutAnnotations(): BoolValue
}

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

public abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLValueType = PartiQLValueType.INT8

    abstract override fun copy(annotations: Annotations): Int8Value

    abstract override fun withAnnotations(annotations: Annotations): Int8Value

    abstract override fun withoutAnnotations(): Int8Value
}

public abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLValueType = PartiQLValueType.INT16

    abstract override fun copy(annotations: Annotations): Int16Value

    abstract override fun withAnnotations(annotations: Annotations): Int16Value

    abstract override fun withoutAnnotations(): Int16Value
}

public abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLValueType = PartiQLValueType.INT32

    abstract override fun copy(annotations: Annotations): Int32Value

    abstract override fun withAnnotations(annotations: Annotations): Int32Value

    abstract override fun withoutAnnotations(): Int32Value
}

public abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLValueType = PartiQLValueType.INT64

    abstract override fun copy(annotations: Annotations): Int64Value

    abstract override fun withAnnotations(annotations: Annotations): Int64Value

    abstract override fun withoutAnnotations(): Int64Value
}

public abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLValueType = PartiQLValueType.INT

    abstract override fun copy(annotations: Annotations): IntValue

    abstract override fun withAnnotations(annotations: Annotations): IntValue

    abstract override fun withoutAnnotations(): IntValue
}

public abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLValueType = PartiQLValueType.DECIMAL

    abstract override fun copy(annotations: Annotations): DecimalValue

    abstract override fun withAnnotations(annotations: Annotations): DecimalValue

    abstract override fun withoutAnnotations(): DecimalValue
}

public abstract class Float32Value : ScalarValue<Float> {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT32
    abstract override fun copy(annotations: Annotations): Float32Value

    abstract override fun withAnnotations(annotations: Annotations): Float32Value

    abstract override fun withoutAnnotations(): Float32Value
}

public abstract class Float64Value : ScalarValue<Double> {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT64

    abstract override fun copy(annotations: Annotations): Float64Value

    abstract override fun withAnnotations(annotations: Annotations): Float64Value

    abstract override fun withoutAnnotations(): Float64Value
}

public sealed class TextValue<T> : ScalarValue<T> {

    public abstract val string: String

    abstract override fun copy(annotations: Annotations): TextValue<T>

    abstract override fun withAnnotations(annotations: Annotations): TextValue<T>

    abstract override fun withoutAnnotations(): TextValue<T>
}

public abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLValueType = PartiQLValueType.CHAR

    override val string: String
        get() = value.toString()

    abstract override fun copy(annotations: Annotations): CharValue

    abstract override fun withAnnotations(annotations: Annotations): CharValue

    abstract override fun withoutAnnotations(): CharValue
}

public abstract class StringValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.STRING

    override val string: String
        get() = value

    abstract override fun copy(annotations: Annotations): StringValue

    abstract override fun withAnnotations(annotations: Annotations): StringValue

    abstract override fun withoutAnnotations(): StringValue
}

public abstract class SymbolValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.SYMBOL

    override val string: String
        get() = value

    abstract override fun copy(annotations: Annotations): SymbolValue

    abstract override fun withAnnotations(annotations: Annotations): SymbolValue

    abstract override fun withoutAnnotations(): SymbolValue
}

public abstract class ClobValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.CLOB

    override val string: String
        get() = value

    abstract override fun copy(annotations: Annotations): ClobValue

    abstract override fun withAnnotations(annotations: Annotations): ClobValue

    abstract override fun withoutAnnotations(): ClobValue
}

public abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLValueType = PartiQLValueType.BINARY

    abstract override fun copy(annotations: Annotations): BinaryValue

    abstract override fun withAnnotations(annotations: Annotations): BinaryValue

    abstract override fun withoutAnnotations(): BinaryValue
}

public abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLValueType = PartiQLValueType.BYTE

    abstract override fun copy(annotations: Annotations): ByteValue

    abstract override fun withAnnotations(annotations: Annotations): ByteValue

    abstract override fun withoutAnnotations(): ByteValue
}

public abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLValueType = PartiQLValueType.BLOB

    abstract override fun copy(annotations: Annotations): BlobValue

    abstract override fun withAnnotations(annotations: Annotations): BlobValue

    abstract override fun withoutAnnotations(): BlobValue
}

public abstract class DateValue : ScalarValue<Date> {

    override val type: PartiQLValueType = PartiQLValueType.DATE

    abstract override fun copy(annotations: Annotations): DateValue

    abstract override fun withAnnotations(annotations: Annotations): DateValue

    abstract override fun withoutAnnotations(): DateValue
}

public abstract class TimeValue : ScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.TIME

    abstract override fun copy(annotations: Annotations): TimeValue

    abstract override fun withAnnotations(annotations: Annotations): TimeValue

    abstract override fun withoutAnnotations(): TimeValue
}

public abstract class TimestampValue : ScalarValue<Instant> {

    override val type: PartiQLValueType = PartiQLValueType.TIMESTAMP

    abstract override fun copy(annotations: Annotations): TimestampValue

    abstract override fun withAnnotations(annotations: Annotations): TimestampValue

    abstract override fun withoutAnnotations(): TimestampValue
}

public abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.INTERVAL

    abstract override fun copy(annotations: Annotations): IntervalValue

    abstract override fun withAnnotations(annotations: Annotations): IntervalValue

    abstract override fun withoutAnnotations(): IntervalValue
}

public abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.BAG

    abstract override fun copy(annotations: Annotations): BagValue<T>

    abstract override fun withAnnotations(annotations: Annotations): BagValue<T>

    abstract override fun withoutAnnotations(): BagValue<T>
}

public abstract class ListValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.LIST

    abstract override fun copy(annotations: Annotations): ListValue<T>

    abstract override fun withAnnotations(annotations: Annotations): ListValue<T>

    abstract override fun withoutAnnotations(): ListValue<T>
}

public abstract class SexpValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.SEXP

    abstract override fun copy(annotations: Annotations): SexpValue<T>

    abstract override fun withAnnotations(annotations: Annotations): SexpValue<T>

    abstract override fun withoutAnnotations(): SexpValue<T>
}

public abstract class StructValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    public abstract val fields: List<Pair<String, T>>

    override val type: PartiQLValueType = PartiQLValueType.STRUCT

    abstract override fun copy(annotations: Annotations): StructValue<T>

    abstract override fun withAnnotations(annotations: Annotations): StructValue<T>

    abstract override fun withoutAnnotations(): StructValue<T>
}

/**
 * Any view over a PartiQLValue
 */
public abstract class AnyValue : PartiQLValue {

    public abstract val value: PartiQLValue

    override val type: PartiQLValueType
        get() = value.type

    abstract override fun copy(annotations: Annotations): AnyValue

    abstract override fun withAnnotations(annotations: Annotations): AnyValue

    abstract override fun withoutAnnotations(): AnyValue
}
