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

@file: JvmName("PartiQL")

package org.partiql.value

import com.amazon.ion.Decimal
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.impl.ArrayValueImpl
import org.partiql.value.impl.BagValueImpl
import org.partiql.value.impl.BinaryValueImpl
import org.partiql.value.impl.BlobValueImpl
import org.partiql.value.impl.BoolValueImpl
import org.partiql.value.impl.ByteValueImpl
import org.partiql.value.impl.CharValueImpl
import org.partiql.value.impl.DateValueImpl
import org.partiql.value.impl.DecimalValueImpl
import org.partiql.value.impl.Float32ValueImpl
import org.partiql.value.impl.Float64ValueImpl
import org.partiql.value.impl.Int16ValueImpl
import org.partiql.value.impl.Int32ValueImpl
import org.partiql.value.impl.Int64ValueImpl
import org.partiql.value.impl.Int8ValueImpl
import org.partiql.value.impl.IntValueImpl
import org.partiql.value.impl.IntervalValueImpl
import org.partiql.value.impl.SexpValueImpl
import org.partiql.value.impl.StringValueImpl
import org.partiql.value.impl.StructValueImpl
import org.partiql.value.impl.TimeValueImpl
import org.partiql.value.impl.TimestampValueImpl
import java.math.BigInteger
import java.time.Instant
import java.util.BitSet
import java.util.Date

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun boolValue(
    value: Boolean,
    annotations: Annotations = emptyList(),
): BoolValue = BoolValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Int8Value(
    value: Byte,
    annotations: Annotations = emptyList(),
): Int8Value = Int8ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Int16Value(
    value: Short,
    annotations: Annotations = emptyList(),
): Int16Value = Int16ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Int32Value(
    value: Int,
    annotations: Annotations = emptyList(),
): Int32Value = Int32ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Int64Value(
    value: Long,
    annotations: Annotations = emptyList(),
): Int64Value = Int64ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun IntValue(
    value: BigInteger,
    annotations: Annotations = emptyList(),
): IntValue = IntValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun DecimalValue(
    value: Decimal,
    annotations: Annotations = emptyList(),
): DecimalValue = DecimalValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Float32Value(
    value: Float,
    annotations: Annotations = emptyList(),
): Float32Value = Float32ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun Float64Value(
    value: Double,
    annotations: Annotations = emptyList(),
): Float64Value = Float64ValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun CharValue(
    value: Char,
    annotations: Annotations = emptyList(),
): CharValue = CharValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun StringValue(
    value: String,
    annotations: Annotations = emptyList(),
): StringValue = StringValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun BinaryValue(
    value: BitSet,
    annotations: Annotations = emptyList(),
): BinaryValue = BinaryValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun ByteValue(
    value: Byte,
    annotations: Annotations = emptyList(),
): ByteValue = ByteValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun BlobValue(
    value: ByteArray,
    annotations: Annotations = emptyList(),
): BlobValue = BlobValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun DateValue(
    value: Date,
    annotations: Annotations = emptyList(),
): DateValue = DateValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun TimeValue(
    value: Long,
    annotations: Annotations = emptyList(),
): TimeValue = TimeValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun TimestampValue(
    value: Instant,
    annotations: Annotations = emptyList(),
): TimestampValue = TimestampValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun IntervalValue(
    value: Long,
    annotations: Annotations = emptyList(),
): IntervalValue = IntervalValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
public fun <T : PartiQLValue> BagValue(
    elements: List<T>,
    annotations: Annotations = emptyList(),
): BagValue<T> = BagValueImpl(elements.toPersistentList(), annotations.toPersistentList())

/**
 * TODO
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
public fun <T : PartiQLValue> ArrayValue(
    elements: List<T>,
    annotations: Annotations = emptyList(),
): ArrayValue<T> = ArrayValueImpl(elements.toPersistentList(), annotations.toPersistentList())

/**
 * TODO
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
public fun <T : PartiQLValue> SexpValue(
    elements: List<T>,
    annotations: Annotations = emptyList(),
): SexpValue<T> = SexpValueImpl(elements.toPersistentList(), annotations.toPersistentList())

/**
 * TODO
 *
 * @param T
 * @param fields
 * @param annotations
 * @return
 */
@JvmOverloads
public fun <T : PartiQLValue> StructValue(
    fields: List<Pair<String, T>>,
    annotations: Annotations = emptyList(),
): StructValue<T> = StructValueImpl(fields.toPersistentList(), annotations.toPersistentList())
