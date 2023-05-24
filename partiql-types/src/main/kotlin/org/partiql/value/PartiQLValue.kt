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

    public object NULL : PartiQLValue {

        override val type: PartiQLType = PartiQLType.NULL
    }

    public object MISSING : PartiQLValue {

        override val type: PartiQLType = PartiQLType.MISSING
    }
}

public interface ScalarValue<T> : PartiQLValue {

    public val value: T
}

public interface CollectionValue<T : PartiQLValue> : PartiQLValue, Collection<T> {

    public override val size: Int

    public val values: Collection<T>
}

public abstract class BoolValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BOOL
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
}

public abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLType = PartiQLType.INT8
}

public abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLType = PartiQLType.INT16
}

public abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLType = PartiQLType.INT32
}

public abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLType = PartiQLType.INT64
}

public abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLType = PartiQLType.INT
}

public abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLType = PartiQLType.DECIMAL
}

public abstract class Float32Value : ScalarValue<Float> {

    override val type: PartiQLType = PartiQLType.FLOAT32
}

public abstract class Float64Value : ScalarValue<Double> {

    override val type: PartiQLType = PartiQLType.FLOAT64
}

public sealed class TextValue<T> : ScalarValue<T> {

    public abstract val string: String
}

public abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLType = PartiQLType.CHAR

    override val string: String
        get() = type.toString()
}

public abstract class StringValue : TextValue<String>() {

    override val type: PartiQLType = PartiQLType.STRING

    override val string: String
        get() = value
}

public abstract class BitValue : ScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BIT
}

public abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLType = PartiQLType.BINARY
}

public abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLType = PartiQLType.BYTE
}

public abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLType = PartiQLType.BLOB
}

public abstract class DateValue : ScalarValue<Date> {

    override val type: PartiQLType = PartiQLType.DATE
}

public abstract class TimeValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.TIME
}

public abstract class TimestampValue : ScalarValue<Instant> {

    override val type: PartiQLType = PartiQLType.TIMESTAMP
}

public abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.INTERVAL
}

public abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.BAG
}

public abstract class ArrayValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.ARRAY
}

public abstract class SexpValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLType = PartiQLType.SEXP
}

public abstract class StructValue<T : PartiQLValue> : PartiQLValue, Collection<Pair<String, T>> {

    public abstract val fields: List<Pair<String, T>>

    override val type: PartiQLType = PartiQLType.STRUCT
}

/**
 * Any view over a PartiQLValue
 */
public abstract class AnyValue : PartiQLValue {

    public abstract val value: PartiQLValue

    override val type: PartiQLType
        get() = value.type
}
