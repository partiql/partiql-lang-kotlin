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
import org.partiql.value.impl.MissingValueImpl
import org.partiql.value.impl.NullValueImpl
import org.partiql.value.impl.NullableBagValueImpl
import org.partiql.value.impl.NullableBinaryValueImpl
import org.partiql.value.impl.NullableBlobValueImpl
import org.partiql.value.impl.NullableBoolValueImpl
import org.partiql.value.impl.NullableByteValueImpl
import org.partiql.value.impl.NullableCharValueImpl
import org.partiql.value.impl.NullableClobValueImpl
import org.partiql.value.impl.NullableDateValueImpl
import org.partiql.value.impl.NullableDecimalValueImpl
import org.partiql.value.impl.NullableFloat32ValueImpl
import org.partiql.value.impl.NullableFloat64ValueImpl
import org.partiql.value.impl.NullableInt16ValueImpl
import org.partiql.value.impl.NullableInt32ValueImpl
import org.partiql.value.impl.NullableInt64ValueImpl
import org.partiql.value.impl.NullableInt8ValueImpl
import org.partiql.value.impl.NullableIntValueImpl
import org.partiql.value.impl.NullableIntervalValueImpl
import org.partiql.value.impl.NullableListValueImpl
import org.partiql.value.impl.NullableSexpValueImpl
import org.partiql.value.impl.NullableStringValueImpl
import org.partiql.value.impl.NullableStructValueImpl
import org.partiql.value.impl.NullableSymbolValueImpl
import org.partiql.value.impl.NullableTimeValueImpl
import org.partiql.value.impl.NullableTimestampValueImpl
import org.partiql.value.impl.SexpValueImpl
import org.partiql.value.impl.StringValueImpl
import org.partiql.value.impl.StructValueImpl
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
    value: Boolean,
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
    value: Byte,
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
    value: Short,
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
    value: Int,
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
    value: Long,
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
    value: BigInteger,
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
    value: BigDecimal,
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
    value: Float,
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
    value: Double,
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
    value: Char,
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
    value: String,
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
    value: String,
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
    value: ByteArray,
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
    value: BitSet,
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
    value: Byte,
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
    value: ByteArray,
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
    value: Date,
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
    value: Time,
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
    value: Timestamp,
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
    value: Long,
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
    elements: List<T>,
    annotations: Annotations = emptyList(),
): BagValue<T> = BagValueImpl(elements.toPersistentList(), annotations.toPersistentList())

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
    elements: List<T>,
    annotations: Annotations = emptyList(),
): ListValue<T> = ListValueImpl(elements.toPersistentList(), annotations.toPersistentList())

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
    elements: List<T>,
    annotations: Annotations = emptyList(),
): SexpValue<T> = SexpValueImpl(elements.toPersistentList(), annotations.toPersistentList())

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
    fields: List<Pair<String, T>>,
    annotations: Annotations = emptyList(),
): StructValue<T> = StructValueImpl(fields.toPersistentList(), annotations.toPersistentList())

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

/**
 * UNION(NULL, BOOL) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableBoolValue(
    value: Boolean? = null,
    annotations: Annotations = emptyList(),
): NullableBoolValue = NullableBoolValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INT8) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableInt8Value(
    value: Byte? = null,
    annotations: Annotations = emptyList(),
): NullableInt8Value = NullableInt8ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INT8) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableInt16Value(
    value: Short? = null,
    annotations: Annotations = emptyList(),
): NullableInt16Value = NullableInt16ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INT16) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableInt32Value(
    value: Int? = null,
    annotations: Annotations = emptyList(),
): NullableInt32Value = NullableInt32ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INT64) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableInt64Value(
    value: Long? = null,
    annotations: Annotations = emptyList(),
): NullableInt64Value = NullableInt64ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INT) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableIntValue(
    value: BigInteger? = null,
    annotations: Annotations = emptyList(),
): NullableIntValue = NullableIntValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, DECIMAL) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableDecimalValue(
    value: BigDecimal? = null,
    annotations: Annotations = emptyList(),
): NullableDecimalValue = NullableDecimalValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, FLOAT32) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableFloat32Value(
    value: Float? = null,
    annotations: Annotations = emptyList(),
): NullableFloat32Value = NullableFloat32ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, FLOAT64) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableFloat64Value(
    value: Double? = null,
    annotations: Annotations = emptyList(),
): NullableFloat64Value = NullableFloat64ValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, CHAR) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableCharValue(
    value: Char? = null,
    annotations: Annotations = emptyList(),
): NullableCharValue = NullableCharValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, STRING) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableStringValue(
    value: String? = null,
    annotations: Annotations = emptyList(),
): NullableStringValue = NullableStringValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, SYMBOL) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableSymbolValue(
    value: String? = null,
    annotations: Annotations = emptyList(),
): NullableSymbolValue = NullableSymbolValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, CLOB) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableClobValue(
    value: ByteArray? = null,
    annotations: Annotations = emptyList(),
): NullableClobValue = NullableClobValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, BINARY) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableBinaryValue(
    value: BitSet? = null,
    annotations: Annotations = emptyList(),
): NullableBinaryValue = NullableBinaryValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, BYTE) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableByteValue(
    value: Byte? = null,
    annotations: Annotations = emptyList(),
): NullableByteValue = NullableByteValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, BLOB) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableBlobValue(
    value: ByteArray? = null,
    annotations: Annotations = emptyList(),
): NullableBlobValue = NullableBlobValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, DATE) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableDateValue(
    value: Date? = null,
    annotations: Annotations = emptyList(),
): NullableDateValue = NullableDateValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, TIME) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableTimeValue(
    value: Time? = null,
    annotations: Annotations = emptyList(),
): NullableTimeValue = NullableTimeValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, TIMESTAMP) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableTimestampValue(
    value: Timestamp? = null,
    annotations: Annotations = emptyList(),
): NullableTimestampValue =
    NullableTimestampValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, INTERVAL) type value.
 *
 * @param value
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun nullableIntervalValue(
    value: Long? = null,
    annotations: Annotations = emptyList(),
): NullableIntervalValue = NullableIntervalValueImpl(value, annotations.toPersistentList())

/**
 * UNION(NULL, BAG) type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> nullableBagValue(
    elements: List<T>? = null,
    annotations: Annotations = emptyList(),
): NullableBagValue<T> = NullableBagValueImpl(elements?.toPersistentList(), annotations.toPersistentList())

/**
 * UNION(NULL, LIST) type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> nullableListValue(
    elements: List<T>? = null,
    annotations: Annotations = emptyList(),
): NullableListValue<T> = NullableListValueImpl(elements?.toPersistentList(), annotations.toPersistentList())

/**
 * UNION(NULL, SEXP) type value.
 *
 * @param T
 * @param elements
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> nullableSexpValue(
    elements: List<T>? = null,
    annotations: Annotations = emptyList(),
): NullableSexpValue<T> = NullableSexpValueImpl(elements?.toPersistentList(), annotations.toPersistentList())

/**
 * UNION(NULL, STRUCT) type value.
 *
 * @param T
 * @param fields
 * @param annotations
 * @return
 */
@JvmOverloads
@PartiQLValueExperimental
public fun <T : PartiQLValue> nullableStructValue(
    fields: List<Pair<String, T>>? = null,
    annotations: Annotations = emptyList(),
): NullableStructValue<T> = NullableStructValueImpl(fields?.toPersistentList(), annotations.toPersistentList())
