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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.BitSet

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

public sealed interface NullableScalarValue<T> : PartiQLValue {

    public val value: T?

    override fun copy(annotations: Annotations): NullableScalarValue<T>

    override fun withAnnotations(annotations: Annotations): NullableScalarValue<T>

    override fun withoutAnnotations(): NullableScalarValue<T>
}

public sealed interface NullableCollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val elements: Collection<T>?

    override fun copy(annotations: Annotations): NullableCollectionValue<T>

    override fun withAnnotations(annotations: Annotations): NullableCollectionValue<T>

    override fun withoutAnnotations(): NullableCollectionValue<T>
}

public abstract class NullableBoolValue : NullableScalarValue<Boolean> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BOOL

    abstract override fun copy(annotations: Annotations): NullableBoolValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBoolValue

    abstract override fun withoutAnnotations(): NullableBoolValue
}

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

public abstract class NullableInt8Value : NullableNumericValue<Byte>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT8

    abstract override fun copy(annotations: Annotations): NullableInt8Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt8Value

    abstract override fun withoutAnnotations(): NullableInt8Value
}

public abstract class NullableInt16Value : NullableNumericValue<Short>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT16

    abstract override fun copy(annotations: Annotations): NullableInt16Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt16Value

    abstract override fun withoutAnnotations(): NullableInt16Value
}

public abstract class NullableInt32Value : NullableNumericValue<Int>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT32

    abstract override fun copy(annotations: Annotations): NullableInt32Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt32Value

    abstract override fun withoutAnnotations(): NullableInt32Value
}

public abstract class NullableInt64Value : NullableNumericValue<Long>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT64

    abstract override fun copy(annotations: Annotations): NullableInt64Value

    abstract override fun withAnnotations(annotations: Annotations): NullableInt64Value

    abstract override fun withoutAnnotations(): NullableInt64Value
}

public abstract class NullableIntValue : NullableNumericValue<BigInteger>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INT

    abstract override fun copy(annotations: Annotations): NullableIntValue

    abstract override fun withAnnotations(annotations: Annotations): NullableIntValue

    abstract override fun withoutAnnotations(): NullableIntValue
}

public abstract class NullableDecimalValue : NullableNumericValue<BigDecimal>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_DECIMAL

    abstract override fun copy(annotations: Annotations): NullableDecimalValue

    abstract override fun withAnnotations(annotations: Annotations): NullableDecimalValue

    abstract override fun withoutAnnotations(): NullableDecimalValue
}

public abstract class NullableFloat32Value : NullableScalarValue<Float> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_FLOAT32

    abstract override fun copy(annotations: Annotations): NullableFloat32Value

    abstract override fun withAnnotations(annotations: Annotations): NullableFloat32Value

    abstract override fun withoutAnnotations(): NullableFloat32Value
}

public abstract class NullableFloat64Value : NullableScalarValue<Double> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_FLOAT64

    abstract override fun copy(annotations: Annotations): NullableFloat64Value

    abstract override fun withAnnotations(annotations: Annotations): NullableFloat64Value

    abstract override fun withoutAnnotations(): NullableFloat64Value
}

public sealed class NullableTextValue<T> : NullableScalarValue<T> {

    public abstract val string: String?

    abstract override fun copy(annotations: Annotations): NullableTextValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableTextValue<T>

    abstract override fun withoutAnnotations(): NullableTextValue<T>
}

public abstract class NullableCharValue : NullableTextValue<Char>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_CHAR

    override val string: String?
        get() = value?.toString()

    abstract override fun copy(annotations: Annotations): NullableCharValue

    abstract override fun withAnnotations(annotations: Annotations): NullableCharValue

    abstract override fun withoutAnnotations(): NullableCharValue
}

public abstract class NullableStringValue : NullableTextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_STRING

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): NullableStringValue

    abstract override fun withAnnotations(annotations: Annotations): NullableStringValue

    abstract override fun withoutAnnotations(): NullableStringValue
}

public abstract class NullableSymbolValue : NullableTextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_SYMBOL

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): NullableSymbolValue

    abstract override fun withAnnotations(annotations: Annotations): NullableSymbolValue

    abstract override fun withoutAnnotations(): NullableSymbolValue
}

public abstract class NullableClobValue : NullableTextValue<ByteArray>() {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_CLOB

    override val string: String?
        get() = value?.toString(Charsets.UTF_8)

    abstract override fun copy(annotations: Annotations): NullableClobValue

    abstract override fun withAnnotations(annotations: Annotations): NullableClobValue

    abstract override fun withoutAnnotations(): NullableClobValue
}

public abstract class NullableBinaryValue : NullableScalarValue<BitSet> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BINARY

    abstract override fun copy(annotations: Annotations): NullableBinaryValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBinaryValue

    abstract override fun withoutAnnotations(): NullableBinaryValue
}

public abstract class NullableByteValue : NullableScalarValue<Byte> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BYTE

    abstract override fun copy(annotations: Annotations): NullableByteValue

    abstract override fun withAnnotations(annotations: Annotations): NullableByteValue

    abstract override fun withoutAnnotations(): NullableByteValue
}

public abstract class NullableBlobValue : NullableScalarValue<ByteArray> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BLOB

    abstract override fun copy(annotations: Annotations): NullableBlobValue

    abstract override fun withAnnotations(annotations: Annotations): NullableBlobValue

    abstract override fun withoutAnnotations(): NullableBlobValue
}

public abstract class NullableDateValue : NullableScalarValue<LocalDate> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_DATE

    abstract override fun copy(annotations: Annotations): NullableDateValue

    abstract override fun withAnnotations(annotations: Annotations): NullableDateValue

    abstract override fun withoutAnnotations(): NullableDateValue
}

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

public abstract class NullableIntervalValue : NullableScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_INTERVAL

    abstract override fun copy(annotations: Annotations): NullableIntervalValue

    abstract override fun withAnnotations(annotations: Annotations): NullableIntervalValue

    abstract override fun withoutAnnotations(): NullableIntervalValue
}

public abstract class NullableBagValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_BAG

    abstract override fun copy(annotations: Annotations): NullableBagValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableBagValue<T>

    abstract override fun withoutAnnotations(): NullableBagValue<T>
}

public abstract class NullableListValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_LIST

    abstract override fun copy(annotations: Annotations): NullableListValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableListValue<T>

    abstract override fun withoutAnnotations(): NullableListValue<T>
}

public abstract class NullableSexpValue<T : PartiQLValue> : NullableCollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_SEXP

    abstract override fun copy(annotations: Annotations): NullableSexpValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableSexpValue<T>

    abstract override fun withoutAnnotations(): NullableSexpValue<T>
}

public abstract class NullableStructValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    public abstract val fields: List<Pair<String, T>>?

    override val type: PartiQLValueType = PartiQLValueType.NULLABLE_STRUCT

    abstract override fun copy(annotations: Annotations): NullableStructValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NullableStructValue<T>

    abstract override fun withoutAnnotations(): NullableStructValue<T>
}
