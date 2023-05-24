/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

import org.partiql.types.PartiQLType
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.BitSet
import java.util.Date

public interface PartiQLValue {

    public val type: PartiQLType

    public val annotations: List<String>

    override fun toString(): String

    public fun withAnnotations(annotations: Iterable<String>): PartiQLValue

    public fun withoutAnnotations(): PartiQLValue
}

public abstract class NullValue : PartiQLValue {

    override val type: PartiQLType = PartiQLType.NULL

    abstract override fun withAnnotations(annotations: Iterable<String>): NullValue

    abstract override fun withoutAnnotations(): NullValue
}

public abstract class MissingValue : PartiQLValue {

    override val type: PartiQLType = PartiQLType.MISSING

    abstract override fun withAnnotations(annotations: Iterable<String>): MissingValue

    abstract override fun withoutAnnotations(): MissingValue
}

public interface ScalarValue<T> : PartiQLValue {

    public val value: T

    override fun withAnnotations(annotations: Iterable<String>): ScalarValue<T>

    override fun withoutAnnotations(): ScalarValue<T>
}

public interface CollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val values: Collection<T>

    override fun withAnnotations(annotations: Iterable<String>): CollectionValue<T>

    override fun withoutAnnotations(): CollectionValue<T>
}

public abstract class BoolValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BOOL

    abstract override fun withAnnotations(annotations: Iterable<String>): BoolValue

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

    override fun toString(): String = value.toString()

    abstract override fun withAnnotations(annotations: Iterable<String>): NumericValue<T>

    abstract override fun withoutAnnotations(): NumericValue<T>
}

public abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLType = PartiQLType.INT8

    abstract override fun withAnnotations(annotations: Iterable<String>): Int8Value

    abstract override fun withoutAnnotations(): Int8Value
}

public abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLType = PartiQLType.INT16

    abstract override fun withAnnotations(annotations: Iterable<String>): Int16Value

    abstract override fun withoutAnnotations(): Int16Value
}

public abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLType = PartiQLType.INT32

    abstract override fun withAnnotations(annotations: Iterable<String>): Int32Value

    abstract override fun withoutAnnotations(): Int32Value
}

public abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLType = PartiQLType.INT64

    abstract override fun withAnnotations(annotations: Iterable<String>): Int64Value

    abstract override fun withoutAnnotations(): Int64Value
}

public abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLType = PartiQLType.INT

    abstract override fun withAnnotations(annotations: Iterable<String>): IntValue

    abstract override fun withoutAnnotations(): IntValue
}

public abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLType = PartiQLType.DECIMAL

    abstract override fun withAnnotations(annotations: Iterable<String>): DecimalValue

    abstract override fun withoutAnnotations(): DecimalValue
}

public abstract class Float32Value : ScalarValue<Float> {

    override val type: PartiQLType = PartiQLType.FLOAT32

    abstract override fun withAnnotations(annotations: Iterable<String>): Float32Value

    abstract override fun withoutAnnotations(): Float32Value
}

public abstract class Float64Value : ScalarValue<Double> {

    override val type: PartiQLType = PartiQLType.FLOAT64

    abstract override fun withAnnotations(annotations: Iterable<String>): Float64Value

    abstract override fun withoutAnnotations(): Float64Value
}

public sealed class TextValue<T> : ScalarValue<T> {

    public abstract val string: String

    abstract override fun withAnnotations(annotations: Iterable<String>): TextValue<T>

    abstract override fun withoutAnnotations(): TextValue<T>
}

public abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLType = PartiQLType.CHAR

    override val string: String
        get() = type.toString()

    abstract override fun withAnnotations(annotations: Iterable<String>): CharValue

    abstract override fun withoutAnnotations(): CharValue
}

public abstract class StringValue : TextValue<String>() {

    override val type: PartiQLType = PartiQLType.STRING

    override val string: String
        get() = value

    abstract override fun withAnnotations(annotations: Iterable<String>): StringValue

    abstract override fun withoutAnnotations(): StringValue
}

public abstract class BitValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BIT

    abstract override fun withAnnotations(annotations: Iterable<String>): BitValue

    abstract override fun withoutAnnotations(): BitValue
}

public abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLType = PartiQLType.BINARY

    abstract override fun withAnnotations(annotations: Iterable<String>): BinaryValue

    abstract override fun withoutAnnotations(): BinaryValue
}

public abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLType = PartiQLType.BYTE

    abstract override fun withAnnotations(annotations: Iterable<String>): ByteValue

    abstract override fun withoutAnnotations(): ByteValue
}

public abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLType = PartiQLType.BLOB

    abstract override fun withAnnotations(annotations: Iterable<String>): BlobValue

    abstract override fun withoutAnnotations(): BlobValue
}

public abstract class DateValue : ScalarValue<Date> {

    override val type: PartiQLType = PartiQLType.DATE

    abstract override fun withAnnotations(annotations: Iterable<String>): DateValue

    abstract override fun withoutAnnotations(): DateValue
}

public abstract class TimeValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.TIME

    abstract override fun withAnnotations(annotations: Iterable<String>): TimeValue

    abstract override fun withoutAnnotations(): TimeValue
}

public abstract class TimestampValue : ScalarValue<Instant> {

    override val type: PartiQLType = PartiQLType.TIMESTAMP

    abstract override fun withAnnotations(annotations: Iterable<String>): TimestampValue

    abstract override fun withoutAnnotations(): TimestampValue
}

public abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.INTERVAL

    abstract override fun withAnnotations(annotations: Iterable<String>): IntervalValue

    abstract override fun withoutAnnotations(): IntervalValue
}

public abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.BAG

    abstract override fun withAnnotations(annotations: Iterable<String>): BagValue<T>

    abstract override fun withoutAnnotations(): BagValue<T>
}

public abstract class ArrayValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.ARRAY

    abstract override fun withAnnotations(annotations: Iterable<String>): ArrayValue<T>

    abstract override fun withoutAnnotations(): ArrayValue<T>
}

public abstract class SexpValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.SEXP

    abstract override fun withAnnotations(annotations: Iterable<String>): SexpValue<T>

    abstract override fun withoutAnnotations(): SexpValue<T>
}

public abstract class StructValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    public abstract val fields: List<Pair<String, T>>

    override val type: PartiQLType = PartiQLType.STRUCT

    abstract override fun withAnnotations(annotations: Iterable<String>): StructValue<T>

    abstract override fun withoutAnnotations(): StructValue<T>
}

/**
 * Any view over a PartiQLValue
 */
public abstract class AnyValue : PartiQLValue {

    public abstract val value: PartiQLValue

    override val type: PartiQLType
        get() = value.type

    abstract override fun withAnnotations(annotations: Iterable<String>): AnyValue

    abstract override fun withoutAnnotations(): AnyValue
}
