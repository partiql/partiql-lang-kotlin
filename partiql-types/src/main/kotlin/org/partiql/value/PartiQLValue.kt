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

interface PartiQLValue {

    public val type: PartiQLType

    object NULL : PartiQLValue {

        override val type: PartiQLType = PartiQLType.NULL
    }

    object MISSING : PartiQLValue {

        override val type: PartiQLType = PartiQLType.NULL
    }
}

interface ScalarValue<T> : PartiQLValue {

    public val value: T
}

interface CollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val values: Collection<T>
}

abstract class BoolValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BOOL
}

sealed class NumericValue<T : Number> : ScalarValue<T> {

    val int: Int
        get() = value.toInt()

    val long: Long
        get() = value.toLong()

    val float: Double
        get() = value.toDouble()

    val double: Float
        get() = value.toFloat()

    override fun toString(): String = value.toString()
}

abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLType = PartiQLType.INT8
}

abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLType = PartiQLType.INT16
}

abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLType = PartiQLType.INT32
}

abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLType = PartiQLType.INT64
}

abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLType = PartiQLType.INT
}

abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLType = PartiQLType.DECIMAL
}

abstract class Float32Value : ScalarValue<Float> {

    override val type: PartiQLType = PartiQLType.FLOAT32
}

abstract class Float64Value : ScalarValue<Double> {

    override val type: PartiQLType = PartiQLType.FLOAT64
}

sealed class TextValue<T> : ScalarValue<T> {

    abstract val string: String

}

abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLType = PartiQLType.CHAR

    override val string: String
        get() = type.toString()
}

abstract class StringValue : TextValue<String>() {

    override val type: PartiQLType = PartiQLType.STRING

    override val string: String
        get() = value
}

abstract class BitValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BIT
}

abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLType = PartiQLType.BINARY
}

abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLType = PartiQLType.BYTE
}

abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLType = PartiQLType.BLOB
}

abstract class DateValue : ScalarValue<Date> {

    override val type: PartiQLType = PartiQLType.DATE
}

abstract class TimeValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.TIME
}

abstract class TimestampValue : ScalarValue<Instant> {

    override val type: PartiQLType = PartiQLType.TIMESTAMP
}

abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.INTERVAL
}

abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.BAG
}

abstract class ArrayValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.ARRAY
}

abstract class TupleValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    abstract val fields: List<Pair<String, T>>

    override val type: PartiQLType = PartiQLType.TUPLE
}

/**
 * Any view over a PartiQLValue
 */
abstract class AnyValue : PartiQLValue {

    abstract val value: PartiQLValue

    override val type: PartiQLType = value.type
}
