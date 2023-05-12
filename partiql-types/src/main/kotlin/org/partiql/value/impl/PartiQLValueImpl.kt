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
import org.partiql.value.ArrayValue
import org.partiql.value.BagValue
import org.partiql.value.BinaryValue
import org.partiql.value.BitValue
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
import org.partiql.value.StringValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.TupleValue
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.BitSet
import java.util.Date

internal data class BoolValueImpl(override val value: Boolean) : BoolValue()

internal data class Int8ValueImpl(override val value: Byte) : Int8Value()

internal data class Int16ValueImpl(override val value: Short) : Int16Value()

internal data class Int32ValueImpl(override val value: Int) : Int32Value()

internal data class Int64ValueImpl(override val value: Long) : Int64Value()

internal data class IntValueImpl(override val value: BigInteger) : IntValue()

internal data class DecimalValueImpl(override val value: BigDecimal) : DecimalValue()

internal data class Float32ValueImpl(override val value: Float) : Float32Value()

internal data class Float64ValueImpl(override val value: Double) : Float64Value()

internal data class CharValueImpl(override val value: Char): CharValue()

internal data class StringValueImpl(override val value: String): StringValue()

internal data class BitValueImpl(override val value: Boolean) : BitValue()

internal data class BinaryValueImpl(override val value: BitSet) : BinaryValue()

internal data class ByteValueImpl(override val value: Byte) : ByteValue()

internal data class BlobValueImpl(override val value: ByteArray) : BlobValue() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlobValueImpl
        return value.contentEquals(other.value)
    }

    override fun hashCode() = value.contentHashCode()
}

internal data class DateValueImpl(override val value: Date): DateValue()

internal data class TimeValueImpl(override val value: Long): TimeValue()

internal data class TimestampValueImpl(override val value: Instant): TimestampValue()

internal data class IntervalValueImpl(override val value: Long): IntervalValue()

internal data class BagValueImpl<T : PartiQLValue>(private val delegate: PersistentList<T>) : BagValue<T>() {

    override fun contains(element: T) = delegate.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator() = delegate.iterator()

    override val size = delegate.size

    override val values = delegate.toImmutableList()
}

internal data class ArrayValueImpl<T : PartiQLValue>(private val delegate: PersistentList<T>) : ArrayValue<T>() {

    override fun contains(element: T) = delegate.contains(element)

    override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator() = delegate.iterator()

    override val size = delegate.size

    override val values = delegate.toImmutableList()
}

internal data class TupleValueImpl<T: PartiQLValue>(private val delegate: PersistentList<Pair<String, T>>) : TupleValue<T>() {

    override val fields = delegate.toImmutableList()

    override val size = delegate.size

    override fun isEmpty() = delegate.isEmpty()

    override fun iterator(): Iterator<Pair<String, T>> = delegate.iterator()

    override fun containsAll(elements: Collection<Pair<String, T>>) = delegate.containsAll(elements)

    override fun contains(element: Pair<String, T>) = delegate.contains(element)
}
