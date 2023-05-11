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

interface PartiQLScalarValue<T> : PartiQLValue {

    public val value: T
}

interface PartiQLCollectionValue<T : PartiQLValue> : PartiQLValue {

    public val size: Int

    public val values: Collection<T>
}

abstract class BoolValue : PartiQLScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BOOL
}

abstract class Int8Value : PartiQLScalarValue<Byte> {

    override val type: PartiQLType = PartiQLType.INT8
}

abstract class Int16Value : PartiQLScalarValue<Short> {

    override val type: PartiQLType = PartiQLType.INT16
}

abstract class Int32Value : PartiQLScalarValue<Int> {

    override val type: PartiQLType = PartiQLType.INT32
}

abstract class Int64Value : PartiQLScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.INT64
}

abstract class IntValue : PartiQLScalarValue<BigInteger> {

    override val type: PartiQLType = PartiQLType.INT
}

abstract class DecimalValue : PartiQLScalarValue<BigDecimal> {

    override val type: PartiQLType = PartiQLType.DECIMAL
}

abstract class Float32Value : PartiQLScalarValue<Float> {

    override val type: PartiQLType = PartiQLType.FLOAT32
}

abstract class Float64Value : PartiQLScalarValue<Double> {

    override val type: PartiQLType = PartiQLType.FLOAT64
}

abstract class CharValue : PartiQLScalarValue<Char> {

    override val type: PartiQLType = PartiQLType.CHAR
}

abstract class StringValue : PartiQLScalarValue<String> {

    override val type: PartiQLType = PartiQLType.STRING
}

abstract class BitValue : PartiQLScalarValue<Boolean> {

    override val type: PartiQLType = PartiQLType.BIT
}

abstract class BinaryValue : PartiQLScalarValue<BitSet> {

    override val type: PartiQLType = PartiQLType.BINARY
}

abstract class ByteValue : PartiQLScalarValue<Byte> {

    override val type: PartiQLType = PartiQLType.BYTE
}

abstract class BlobValue : PartiQLScalarValue<BlobValue> {

    override val type: PartiQLType = PartiQLType.BLOB
}

abstract class DateValue : PartiQLScalarValue<Date> {

    override val type: PartiQLType = PartiQLType.DATE
}

abstract class TimeValue : PartiQLScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.TIME
}

abstract class TimestampValue : PartiQLScalarValue<Instant> {

    override val type: PartiQLType = PartiQLType.TIMESTAMP
}

abstract class IntervalValue : PartiQLScalarValue<Long> {

    override val type: PartiQLType = PartiQLType.INTERVAL
}

abstract class BagValue<T : PartiQLValue> : PartiQLCollectionValue<T> {

    override val type: PartiQLType = PartiQLType.BAG
}

abstract class ArrayValue<T : PartiQLValue> : PartiQLCollectionValue<T> {

    override val type: PartiQLType = PartiQLType.ARRAY
}

abstract class TupleValue<T: PartiQLValue> : PartiQLCollectionValue<T> {

    abstract val fields: List<Pair<String, T>>

    override val type: PartiQLType = PartiQLType.TUPLE
}
