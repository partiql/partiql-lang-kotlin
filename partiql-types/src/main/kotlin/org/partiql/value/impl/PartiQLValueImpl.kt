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

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.ArrayValue
import org.partiql.value.BagValue
import org.partiql.value.BinaryValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.ByteValue
import org.partiql.value.CharValue
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
import org.partiql.value.PartiQLValue
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.BitSet
import java.util.Date

@Suppress("FunctionName")
internal inline fun <reified T : PartiQLValue> T._withAnnotations(annotations: Annotations): T =
    when {
        annotations.isEmpty() -> this
        else -> copy(annotations = this.annotations + annotations) as T
    }

@Suppress("FunctionName")
internal inline fun <reified T : PartiQLValue> T._withoutAnnotations(): T =
    when {
        this.annotations.isNotEmpty() -> copy(annotations = emptyList()) as T
        else -> this
    }

internal data class BoolValueImpl(
    override val value: Boolean,
    override val annotations: PersistentList<String>,
) : BoolValue() {

    override fun copy(annotations: Annotations) = BoolValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BoolValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BoolValue = _withoutAnnotations()
}

internal data class Int8ValueImpl(
    override val value: Byte,
    override val annotations: PersistentList<String>,
) : Int8Value() {

    override fun copy(annotations: Annotations) = Int8ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int8Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int8Value = _withoutAnnotations()
}

internal data class Int16ValueImpl(
    override val value: Short,
    override val annotations: PersistentList<String>,
) : Int16Value() {

    override fun copy(annotations: Annotations) = Int16ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int16Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int16Value = _withoutAnnotations()
}

internal data class Int32ValueImpl(
    override val value: Int,
    override val annotations: PersistentList<String>,
) : Int32Value() {

    override fun copy(annotations: Annotations) = Int32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int32Value = _withoutAnnotations()
}

internal data class Int64ValueImpl(
    override val value: Long,
    override val annotations: PersistentList<String>,
) : Int64Value() {
    override fun copy(annotations: Annotations) = Int64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int64Value = _withoutAnnotations()
}

internal data class IntValueImpl(
    override val value: BigInteger,
    override val annotations: PersistentList<String>,
) : IntValue() {

    override fun copy(annotations: Annotations) = IntValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): IntValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): IntValue = _withoutAnnotations()
}

internal data class DecimalValueImpl(
    override val value: BigDecimal,
    override val annotations: PersistentList<String>,
) : DecimalValue() {

    override fun copy(annotations: Annotations) = DecimalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): DecimalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): DecimalValue = _withoutAnnotations()
}

internal data class Float32ValueImpl(
    override val value: Float,
    override val annotations: PersistentList<String>,
) : Float32Value() {

    override fun copy(annotations: Annotations) = Float32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Float32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Float32Value = _withoutAnnotations()
}

internal data class Float64ValueImpl(
    override val value: Double,
    override val annotations: PersistentList<String>,
) : Float64Value() {
    override fun copy(annotations: Annotations) = Float64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Float64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Float64Value = _withoutAnnotations()
}

internal data class CharValueImpl(
    override val value: Char,
    override val annotations: PersistentList<String>,
) : CharValue() {
    override fun copy(annotations: Annotations) = CharValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): CharValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): CharValue = _withoutAnnotations()
}

internal data class StringValueImpl(
    override val value: String,
    override val annotations: PersistentList<String>,
) : StringValue() {
    override fun copy(annotations: Annotations) = StringValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StringValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): StringValue = _withoutAnnotations()
}

internal data class BinaryValueImpl(
    override val value: BitSet,
    override val annotations: PersistentList<String>,
) : BinaryValue() {
    override fun copy(annotations: Annotations) = BinaryValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BinaryValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BinaryValue = _withoutAnnotations()
}

internal data class ByteValueImpl(
    override val value: Byte,
    override val annotations: PersistentList<String>,
) : ByteValue() {
    override fun copy(annotations: Annotations) = ByteValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): ByteValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): ByteValue = _withoutAnnotations()
}

internal data class BlobValueImpl(
    override val value: ByteArray,
    override val annotations: PersistentList<String>,
) : BlobValue() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlobValueImpl
        return value.contentEquals(other.value)
    }

    override fun hashCode() = value.contentHashCode()
    override fun copy(annotations: Annotations) = BlobValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BlobValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BlobValue = _withoutAnnotations()
}

internal data class DateValueImpl(
    override val value: Date,
    override val annotations: PersistentList<String>,
) : DateValue() {
    override fun copy(annotations: Annotations) = DateValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): DateValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): DateValue = _withoutAnnotations()
}

internal data class TimeValueImpl(
    override val value: Long,
    override val annotations: PersistentList<String>,
) : TimeValue() {
    override fun copy(annotations: Annotations) = TimeValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): TimeValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): TimeValue = _withoutAnnotations()
}

internal data class TimestampValueImpl(
    override val value: Instant,
    override val annotations: PersistentList<String>,
) : TimestampValue() {
    override fun copy(annotations: Annotations) = TimestampValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): TimestampValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): TimestampValue = _withoutAnnotations()
}

internal data class IntervalValueImpl(
    override val value: Long,
    override val annotations: PersistentList<String>,
) : IntervalValue() {
    override fun copy(annotations: Annotations) = IntervalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): IntervalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): IntervalValue = _withoutAnnotations()
}

internal data class BagValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>,
    override val annotations: PersistentList<String>,
) : BagValue<T>() {

    override fun contains(element: T) = delegate.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator() = delegate.iterator()

    override val size = delegate.size

    override val elements = delegate.toImmutableList()

    override fun copy(annotations: Annotations) = BagValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BagValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): BagValue<T> = _withoutAnnotations()
}

internal data class ArrayValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>,
    override val annotations: PersistentList<String>,
) : ArrayValue<T>() {

    override fun contains(element: T) = delegate.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator() = delegate.iterator()

    override val size = delegate.size

    override val elements = delegate.toImmutableList()

    override fun copy(annotations: Annotations) = ArrayValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): ArrayValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): ArrayValue<T> = _withoutAnnotations()
}

internal data class SexpValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>,
    override val annotations: PersistentList<String>,
) : SexpValue<T>() {

    override fun contains(element: T) = delegate.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator() = delegate.iterator()

    override val size = delegate.size

    override val elements = delegate.toImmutableList()

    override fun copy(annotations: Annotations) = SexpValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): SexpValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): SexpValue<T> = _withoutAnnotations()
}

/**
 * Map
 *
 * @param T
 * @property values
 * @property annotations
 */
internal data class StructValueImpl<T : PartiQLValue>(
    private val values: PersistentList<Pair<String, T>>,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val fields = values.toImmutableList()

    override val size = values.size

    override fun isEmpty() = values.isEmpty()

    override fun iterator(): Iterator<Pair<String, T>> = values.iterator()

    override fun containsAll(elements: Collection<Pair<String, T>>) = values.containsAll(elements)

    override fun contains(element: Pair<String, T>) = values.contains(element)

    override fun copy(annotations: Annotations) = StructValueImpl(values, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): StructValue<T> = _withoutAnnotations()
}
