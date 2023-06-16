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

import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.impl.BagValueImpl
import org.partiql.value.impl.BinaryValueImpl
import org.partiql.value.impl.BlobValueImpl
import org.partiql.value.impl.BoolValueImpl
import org.partiql.value.impl.ByteValueImpl
import org.partiql.value.impl.CharValueImpl
import org.partiql.value.impl.ClobValueImpl
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
import org.partiql.value.impl.ListValueImpl
import org.partiql.value.impl.MissingValueImpl
import org.partiql.value.impl.NullValueImpl
import org.partiql.value.impl.SexpValueImpl
import org.partiql.value.impl.StringValueImpl
import org.partiql.value.impl.StructValueImpl
import org.partiql.value.impl.SymbolValueImpl
import org.partiql.value.impl.TimeValueImpl
import org.partiql.value.impl.TimestampValueImpl
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.BitSet

/**
 * TODO
 *
 * @param annotations
 * @return
 */
@JvmOverloads
public fun nullValue(
    annotations: Annotations = emptyList(),
): NullValue = NullValueImpl(annotations.toPersistentList())

/**
 * TODO
 *
 * @param annotations
 * @return
 */
@JvmOverloads
public fun missingValue(
    annotations: Annotations = emptyList(),
): MissingValue = MissingValueImpl(annotations.toPersistentList())

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
public fun int8Value(
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
public fun int16Value(
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
public fun int32Value(
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
public fun int64Value(
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
public fun intValue(
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
public fun decimalValue(
    value: BigDecimal,
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
public fun float32Value(
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
public fun float64Value(
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
public fun charValue(
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
public fun stringValue(
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
public fun symbolValue(
    value: String,
    annotations: Annotations = emptyList(),
): SymbolValue = SymbolValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun clobValue(
    value: ByteArray,
    annotations: Annotations = emptyList(),
): ClobValue = ClobValueImpl(value, annotations.toPersistentList())

/**
 * TODO
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
public fun binaryValue(
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
public fun byteValue(
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
public fun blobValue(
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
public fun dateValue(
    value: LocalDate,
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
public fun timeValue(
    value: LocalTime,
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
public fun timestampValue(
    value: LocalDateTime,
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
public fun intervalValue(
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
public fun <T : PartiQLValue> bagValue(
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
public fun <T : PartiQLValue> listValue(
    elements: List<T>,
    annotations: Annotations = emptyList(),
): ListValue<T> = ListValueImpl(elements.toPersistentList(), annotations.toPersistentList())

/**
 * TODO
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
public fun <T : PartiQLValue> sexpValue(
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
public fun <T : PartiQLValue> structValue(
    fields: List<Pair<String, T>>,
    annotations: Annotations = emptyList(),
): StructValue<T> = StructValueImpl(fields.toPersistentList(), annotations.toPersistentList())
