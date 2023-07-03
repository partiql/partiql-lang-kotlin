@file:OptIn(PartiQLValueExperimental::class)

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

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
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
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableStructValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.NullableTimeValue
import org.partiql.value.NullableTimestampValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.Timestamp
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal
import java.math.BigInteger
import java.util.BitSet

internal data class NullValueImpl(
    override val annotations: PersistentList<String>,
) : NullValue() {

    override fun copy(annotations: Annotations) = NullValueImpl(annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNull(this, ctx)
}

internal data class MissingValueImpl(
    override val annotations: PersistentList<String>,
) : MissingValue() {

    override fun copy(annotations: Annotations) = MissingValueImpl(annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): MissingValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): MissingValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitMissing(this, ctx)
}

internal data class NullableBoolValueImpl(
    override val value: Boolean?,
    override val annotations: PersistentList<String>,
) : NullableBoolValue() {

    override fun copy(annotations: Annotations) = NullableBoolValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableBoolValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableBoolValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableBool(this, ctx)
}

internal data class NullableInt8ValueImpl(
    override val value: Byte?,
    override val annotations: PersistentList<String>,
) : NullableInt8Value() {

    override fun copy(annotations: Annotations) = NullableInt8ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableInt8Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableInt8Value = _withoutAnnotations()
    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInt8(this, ctx)
}

internal data class NullableInt16ValueImpl(
    override val value: Short?,
    override val annotations: PersistentList<String>,
) : NullableInt16Value() {

    override fun copy(annotations: Annotations) = NullableInt16ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableInt16Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableInt16Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInt16(this, ctx)
}

internal data class NullableInt32ValueImpl(
    override val value: Int?,
    override val annotations: PersistentList<String>,
) : NullableInt32Value() {

    override fun copy(annotations: Annotations) = NullableInt32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableInt32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableInt32Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInt32(this, ctx)
}

internal data class NullableInt64ValueImpl(
    override val value: Long?,
    override val annotations: PersistentList<String>,
) : NullableInt64Value() {
    override fun copy(annotations: Annotations) = NullableInt64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableInt64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableInt64Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInt64(this, ctx)
}

internal data class NullableIntValueImpl(
    override val value: BigInteger?,
    override val annotations: PersistentList<String>,
) : NullableIntValue() {

    override fun copy(annotations: Annotations) = NullableIntValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableIntValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableIntValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInt(this, ctx)
}

internal data class NullableDecimalValueImpl(
    override val value: BigDecimal?,
    override val annotations: PersistentList<String>,
) : NullableDecimalValue() {

    override fun copy(annotations: Annotations) = NullableDecimalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableDecimalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableDecimalValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableDecimal(this, ctx)
}

internal data class NullableFloat32ValueImpl(
    override val value: Float?,
    override val annotations: PersistentList<String>,
) : NullableFloat32Value() {

    override fun copy(annotations: Annotations) = NullableFloat32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableFloat32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableFloat32Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableFloat32(this, ctx)
}

internal data class NullableFloat64ValueImpl(
    override val value: Double?,
    override val annotations: PersistentList<String>,
) : NullableFloat64Value() {
    override fun copy(annotations: Annotations) = NullableFloat64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableFloat64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableFloat64Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableFloat64(this, ctx)
}

internal data class NullableCharValueImpl(
    override val value: Char?,
    override val annotations: PersistentList<String>,
) : NullableCharValue() {

    override fun copy(annotations: Annotations) = NullableCharValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableCharValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableCharValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableChar(this, ctx)
}

internal data class NullableStringValueImpl(
    override val value: String?,
    override val annotations: PersistentList<String>,
) : NullableStringValue() {
    override fun copy(annotations: Annotations) = NullableStringValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableStringValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableStringValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableString(this, ctx)
}

internal data class NullableSymbolValueImpl(
    override val value: String?,
    override val annotations: PersistentList<String>,
) : NullableSymbolValue() {
    override fun copy(annotations: Annotations) = NullableSymbolValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableSymbolValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableSymbolValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableSymbol(this, ctx)
}

internal data class NullableClobValueImpl(
    override val value: ByteArray?,
    override val annotations: PersistentList<String>,
) : NullableClobValue() {
    override fun copy(annotations: Annotations) = NullableClobValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableClobValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableClobValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableClob(this, ctx)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NullableClobValueImpl
        if (value != null) {
            if (other.value == null) return false
            if (!value.contentEquals(other.value)) return false
        } else if (other.value != null) return false
        return annotations == other.annotations
    }

    override fun hashCode(): Int {
        var result = value?.contentHashCode() ?: 0
        result = 31 * result + annotations.hashCode()
        return result
    }
}

internal data class NullableBinaryValueImpl(
    override val value: BitSet?,
    override val annotations: PersistentList<String>,
) : NullableBinaryValue() {
    override fun copy(annotations: Annotations) = NullableBinaryValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableBinaryValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableBinaryValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableBinary(this, ctx)
}

internal data class NullableByteValueImpl(
    override val value: Byte?,
    override val annotations: PersistentList<String>,
) : NullableByteValue() {
    override fun copy(annotations: Annotations) = NullableByteValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableByteValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableByteValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableByte(this, ctx)
}

internal data class NullableBlobValueImpl(
    override val value: ByteArray?,
    override val annotations: PersistentList<String>,
) : NullableBlobValue() {

    override fun copy(annotations: Annotations) = NullableBlobValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableBlobValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableBlobValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableBlob(this, ctx)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NullableClobValueImpl
        if (value != null) {
            if (other.value == null) return false
            if (!value.contentEquals(other.value)) return false
        } else if (other.value != null) return false
        return annotations == other.annotations
    }

    override fun hashCode(): Int {
        var result = value?.contentHashCode() ?: 0
        result = 31 * result + annotations.hashCode()
        return result
    }
}

internal data class NullableDateValueImpl(
    override val value: Date?,
    override val annotations: PersistentList<String>,
) : NullableDateValue() {
    override fun copy(annotations: Annotations) = NullableDateValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableDateValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableDateValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableDate(this, ctx)
}

internal data class NullableTimeValueImpl(
    override val value: Time?,
    override val annotations: PersistentList<String>,
) : NullableTimeValue() {
    override fun copy(annotations: Annotations) =
        NullableTimeValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableTimeValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableTimeValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableTime(this, ctx)
}

internal data class NullableTimestampValueImpl(
    override val value: Timestamp?,
    override val annotations: PersistentList<String>,
) : NullableTimestampValue() {
    override fun copy(annotations: Annotations) =
        NullableTimestampValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableTimestampValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableTimestampValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R =
        visitor.visitNullableTimestamp(this, ctx)
}

internal data class NullableIntervalValueImpl(
    override val value: Long?,
    override val annotations: PersistentList<String>,
) : NullableIntervalValue() {
    override fun copy(annotations: Annotations) = NullableIntervalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableIntervalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableIntervalValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableInterval(this, ctx)
}

internal data class NullableBagValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>?,
    override val annotations: PersistentList<String>,
) : NullableBagValue<T>() {

    override fun contains(element: T) = delegate!!.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate!!.containsAll(elements)

    override fun isEmpty() = delegate!!.isEmpty()

    override fun iterator() = delegate!!.iterator()

    override val size = delegate!!.size

    override val elements = delegate!!.toImmutableList()

    override fun copy(annotations: Annotations) = NullableBagValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableBagValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableBagValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableBag(this, ctx)
}

internal data class NullableListValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>?,
    override val annotations: PersistentList<String>,
) : NullableListValue<T>() {

    override fun contains(element: T) = delegate!!.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate!!.containsAll(elements)

    override fun isEmpty() = delegate!!.isEmpty()

    override fun iterator() = delegate!!.iterator()

    override val size = delegate!!.size

    override val elements = delegate!!.toImmutableList()

    override fun copy(annotations: Annotations) = NullableListValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableListValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableListValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableList(this, ctx)
}

internal data class NullableSexpValueImpl<T : PartiQLValue>(
    private val delegate: PersistentList<T>?,
    override val annotations: PersistentList<String>,
) : NullableSexpValue<T>() {

    override fun contains(element: T) = delegate!!.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate!!.containsAll(elements)

    override fun isEmpty() = delegate!!.isEmpty()

    override fun iterator() = delegate!!.iterator()

    override val size = delegate!!.size

    override val elements = delegate!!.toImmutableList()

    override fun copy(annotations: Annotations) = NullableSexpValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableSexpValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableSexpValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableSexp(this, ctx)
}

internal data class NullableStructValueImpl<T : PartiQLValue>(
    private val values: PersistentList<Pair<String, T>>?,
    override val annotations: PersistentList<String>,
) : NullableStructValue<T>() {

    override val fields = values!!.toImmutableList()

    override val size = values!!.size

    override fun isEmpty() = values!!.isEmpty()

    override fun iterator(): Iterator<Pair<String, T>> = values!!.iterator()

    override fun containsAll(elements: Collection<Pair<String, T>>) = values!!.containsAll(elements)

    override fun contains(element: Pair<String, T>) = values!!.contains(element)

    override fun copy(annotations: Annotations) = NullableStructValueImpl(values, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullableStructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullableStructValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNullableStruct(this, ctx)
}
