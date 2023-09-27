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

// Not strictly necessary, but is explicit
@file: JvmName("PartiQL")

package org.partiql.value

import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.Timestamp
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
import org.partiql.value.impl.MapStructValueImpl
import org.partiql.value.impl.MissingValueImpl
import org.partiql.value.impl.MultiMapStructValueImpl
import org.partiql.value.impl.NullValueImpl
import org.partiql.value.impl.SequenceStructValueImpl
import org.partiql.value.impl.SexpValueImpl
import org.partiql.value.impl.StringValueImpl
import org.partiql.value.impl.SymbolValueImpl
import org.partiql.value.impl.TimeValueImpl
import org.partiql.value.impl.TimestampValueImpl
import java.math.BigDecimal
import java.math.BigInteger
import java.util.BitSet

/**
 * BOOL type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun boolValue(
    value: Boolean?,
    annotations: Annotations = emptyList(),
): BoolValue = BoolValueImpl(value, annotations.toPersistentList())

/**
 * INT8 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun int8Value(
    value: Byte?,
    annotations: Annotations = emptyList(),
): Int8Value = Int8ValueImpl(value, annotations.toPersistentList())

/**
 * INT16 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun int16Value(
    value: Short?,
    annotations: Annotations = emptyList(),
): Int16Value = Int16ValueImpl(value, annotations.toPersistentList())

/**
 * INT32 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun int32Value(
    value: Int?,
    annotations: Annotations = emptyList(),
): Int32Value = Int32ValueImpl(value, annotations.toPersistentList())

/**
 * INT64 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun int64Value(
    value: Long?,
    annotations: Annotations = emptyList(),
): Int64Value = Int64ValueImpl(value, annotations.toPersistentList())

/**
 * INT type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun intValue(
    value: BigInteger?,
    annotations: Annotations = emptyList(),
): IntValue = IntValueImpl(value, annotations.toPersistentList())

/**
 * DECIMAL type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun decimalValue(
    value: BigDecimal?,
    annotations: Annotations = emptyList(),
): DecimalValue = DecimalValueImpl(value, annotations.toPersistentList())

/**
 * FLOAT32 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun float32Value(
    value: Float?,
    annotations: Annotations = emptyList(),
): Float32Value = Float32ValueImpl(value, annotations.toPersistentList())

/**
 * FLOAT64 type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun float64Value(
    value: Double?,
    annotations: Annotations = emptyList(),
): Float64Value = Float64ValueImpl(value, annotations.toPersistentList())

/**
 * CHAR type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun charValue(
    value: Char?,
    annotations: Annotations = emptyList(),
): CharValue = CharValueImpl(value, annotations.toPersistentList())

/**
 * STRING type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun stringValue(
    value: String?,
    annotations: Annotations = emptyList(),
): StringValue = StringValueImpl(value, annotations.toPersistentList())

/**
 * SYMBOL type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun symbolValue(
    value: String?,
    annotations: Annotations = emptyList(),
): SymbolValue = SymbolValueImpl(value, annotations.toPersistentList())

/**
 * CLOB type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun clobValue(
    value: ByteArray?,
    annotations: Annotations = emptyList(),
): ClobValue = ClobValueImpl(value, annotations.toPersistentList())

/**
 * BINARY type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun binaryValue(
    value: BitSet?,
    annotations: Annotations = emptyList(),
): BinaryValue = BinaryValueImpl(value, annotations.toPersistentList())

/**
 * BYTE type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun byteValue(
    value: Byte?,
    annotations: Annotations = emptyList(),
): ByteValue = ByteValueImpl(value, annotations.toPersistentList())

/**
 * BLOB type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun blobValue(
    value: ByteArray?,
    annotations: Annotations = emptyList(),
): BlobValue = BlobValueImpl(value, annotations.toPersistentList())

/**
 * DATE type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun dateValue(
    value: Date?,
    annotations: Annotations = emptyList(),
): DateValue = DateValueImpl(value, annotations.toPersistentList())

/**
 * TIME type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun timeValue(
    value: Time?,
    annotations: Annotations = emptyList(),
): TimeValue = TimeValueImpl(value, annotations.toPersistentList())

/**
 * TIMESTAMP type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun timestampValue(
    value: Timestamp?,
    annotations: Annotations = emptyList(),
): TimestampValue = TimestampValueImpl(value, annotations.toPersistentList())

/**
 * INTERVAL type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun intervalValue(
    value: Long?,
    annotations: Annotations = emptyList(),
): IntervalValue = IntervalValueImpl(value, annotations.toPersistentList())

/**
 * BAG type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> bagValue(
    elements: Sequence<T>?,
    annotations: Annotations = emptyList(),
): BagValue<T> = BagValueImpl(elements, annotations.toPersistentList())

/**
 * LIST type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> listValue(
    elements: Sequence<T>?,
    annotations: Annotations = emptyList(),
): ListValue<T> = ListValueImpl(elements, annotations.toPersistentList())

/**
 * SEXP type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> sexpValue(
    elements: Sequence<T>?,
    annotations: Annotations = emptyList(),
): SexpValue<T> = SexpValueImpl(elements, annotations.toPersistentList())

/**
 * STRUCT type value.
 *
 * @param T
 * @param fields
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> structValue(
    fields: Sequence<Pair<String, T>>?,
    annotations: Annotations = emptyList(),
): StructValue<T> = SequenceStructValueImpl(fields, annotations.toPersistentList())

/**
 * STRUCT type value.
 *
 * @param T
 * @param fields
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> structValueWithDuplicates(
    fields: Map<String, Iterable<T>>?,
    annotations: Annotations = emptyList(),
): StructValue<T> = MultiMapStructValueImpl(fields, annotations.toPersistentList())

/**
 * STRUCT type value.
 *
 * @param T
 * @param fields
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> structValueNoDuplicates(
    fields: Map<String, T>?,
    annotations: Annotations = emptyList(),
): StructValue<T> = MapStructValueImpl(fields, annotations.toPersistentList())

/**
 * NULL type value.
 *
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullValue(
    annotations: Annotations = emptyList(),
): NullValue = NullValueImpl(annotations.toPersistentList())

/**
 * MISSING type value.
 *
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun missingValue(
    annotations: Annotations = emptyList(),
): MissingValue = MissingValueImpl(annotations.toPersistentList())
