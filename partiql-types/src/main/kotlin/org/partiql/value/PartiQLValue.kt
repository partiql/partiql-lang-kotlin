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

package org.partiql.value

import com.amazon.ionelement.api.IonElement
import org.partiql.errors.TypeCheckException
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.Timestamp
import org.partiql.value.helpers.ToIon
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal
import java.math.BigInteger
import java.util.BitSet

internal typealias Annotations = List<String>

/**
 * TODO
 *  - Implement ANY
 *  - Implement comparators
 */
@PartiQLValueExperimental
public sealed interface PartiQLValue {

    public val type: PartiQLValueType

    public val annotations: Annotations

    public val isNull: Boolean

    public fun copy(annotations: Annotations = this.annotations): PartiQLValue

    public fun withAnnotations(annotations: Annotations): PartiQLValue

    public fun withoutAnnotations(): PartiQLValue

    public fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R
}

@PartiQLValueExperimental
public sealed interface ScalarValue<T> : PartiQLValue {

    public val value: T?

    override val isNull: Boolean
        get() = value == null

    override fun copy(annotations: Annotations): ScalarValue<T>

    override fun withAnnotations(annotations: Annotations): ScalarValue<T>

    override fun withoutAnnotations(): ScalarValue<T>
}

@PartiQLValueExperimental
public sealed interface CollectionValue<T : PartiQLValue> : PartiQLValue, Iterable<T> {

    override val isNull: Boolean

    override fun iterator(): Iterator<T>

    override fun copy(annotations: Annotations): CollectionValue<T>

    override fun withAnnotations(annotations: Annotations): CollectionValue<T>

    override fun withoutAnnotations(): CollectionValue<T>
}

@PartiQLValueExperimental
public abstract class BoolValue : ScalarValue<Boolean?> {

    override val type: PartiQLValueType = PartiQLValueType.BOOL

    abstract override fun copy(annotations: Annotations): BoolValue

    abstract override fun withAnnotations(annotations: Annotations): BoolValue

    abstract override fun withoutAnnotations(): BoolValue
}

@PartiQLValueExperimental
public sealed class NumericValue<T : Number> : ScalarValue<T> {

    public abstract fun toInt8(): Int8Value

    public abstract fun toInt16(): Int16Value

    public abstract fun toInt32(): Int32Value

    public abstract fun toInt64(): Int64Value

    public abstract fun toInt(): IntValue

    public abstract fun toDecimal(): DecimalValue

    public abstract fun toFloat32(): Float32Value

    public abstract fun toFloat64(): Float64Value

    abstract override fun copy(annotations: Annotations): NumericValue<T>

    abstract override fun withAnnotations(annotations: Annotations): NumericValue<T>

    abstract override fun withoutAnnotations(): NumericValue<T>
}

@PartiQLValueExperimental
public abstract class Int8Value : NumericValue<Byte>() {

    override val type: PartiQLValueType = PartiQLValueType.INT8

    abstract override fun copy(annotations: Annotations): Int8Value

    abstract override fun withAnnotations(annotations: Annotations): Int8Value

    abstract override fun withoutAnnotations(): Int8Value
}

@PartiQLValueExperimental
public abstract class Int16Value : NumericValue<Short>() {

    override val type: PartiQLValueType = PartiQLValueType.INT16

    abstract override fun copy(annotations: Annotations): Int16Value

    abstract override fun withAnnotations(annotations: Annotations): Int16Value

    abstract override fun withoutAnnotations(): Int16Value
}

@PartiQLValueExperimental
public abstract class Int32Value : NumericValue<Int>() {

    override val type: PartiQLValueType = PartiQLValueType.INT32

    abstract override fun copy(annotations: Annotations): Int32Value

    abstract override fun withAnnotations(annotations: Annotations): Int32Value

    abstract override fun withoutAnnotations(): Int32Value
}

@PartiQLValueExperimental
public abstract class Int64Value : NumericValue<Long>() {

    override val type: PartiQLValueType = PartiQLValueType.INT64

    abstract override fun copy(annotations: Annotations): Int64Value

    abstract override fun withAnnotations(annotations: Annotations): Int64Value

    abstract override fun withoutAnnotations(): Int64Value
}

@PartiQLValueExperimental
public abstract class IntValue : NumericValue<BigInteger>() {

    override val type: PartiQLValueType = PartiQLValueType.INT

    abstract override fun copy(annotations: Annotations): IntValue

    abstract override fun withAnnotations(annotations: Annotations): IntValue

    abstract override fun withoutAnnotations(): IntValue
}

@PartiQLValueExperimental
public abstract class DecimalValue : NumericValue<BigDecimal>() {

    override val type: PartiQLValueType = PartiQLValueType.DECIMAL_ARBITRARY

    abstract override fun copy(annotations: Annotations): DecimalValue

    abstract override fun withAnnotations(annotations: Annotations): DecimalValue

    abstract override fun withoutAnnotations(): DecimalValue
}

@PartiQLValueExperimental
public abstract class Float32Value : NumericValue<Float>() {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT32

    abstract override fun copy(annotations: Annotations): Float32Value

    abstract override fun withAnnotations(annotations: Annotations): Float32Value

    abstract override fun withoutAnnotations(): Float32Value
}

@PartiQLValueExperimental
public abstract class Float64Value : NumericValue<Double>() {

    override val type: PartiQLValueType = PartiQLValueType.FLOAT64

    abstract override fun copy(annotations: Annotations): Float64Value

    abstract override fun withAnnotations(annotations: Annotations): Float64Value

    abstract override fun withoutAnnotations(): Float64Value
}

@PartiQLValueExperimental
public sealed class TextValue<T> : ScalarValue<T> {

    public abstract val string: String?

    abstract override fun copy(annotations: Annotations): TextValue<T>

    abstract override fun withAnnotations(annotations: Annotations): TextValue<T>

    abstract override fun withoutAnnotations(): TextValue<T>
}

@PartiQLValueExperimental
public abstract class CharValue : TextValue<Char>() {

    override val type: PartiQLValueType = PartiQLValueType.CHAR

    override val string: String?
        get() = value?.toString()

    abstract override fun copy(annotations: Annotations): CharValue

    abstract override fun withAnnotations(annotations: Annotations): CharValue

    abstract override fun withoutAnnotations(): CharValue
}

@PartiQLValueExperimental
public abstract class StringValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.STRING

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): StringValue

    abstract override fun withAnnotations(annotations: Annotations): StringValue

    abstract override fun withoutAnnotations(): StringValue
}

@PartiQLValueExperimental
public abstract class SymbolValue : TextValue<String>() {

    override val type: PartiQLValueType = PartiQLValueType.SYMBOL

    override val string: String?
        get() = value

    abstract override fun copy(annotations: Annotations): SymbolValue

    abstract override fun withAnnotations(annotations: Annotations): SymbolValue

    abstract override fun withoutAnnotations(): SymbolValue
}

@PartiQLValueExperimental
public abstract class ClobValue : TextValue<ByteArray>() {

    override val type: PartiQLValueType = PartiQLValueType.CLOB

    override val string: String?
        get() = value?.toString(Charsets.UTF_8)

    abstract override fun copy(annotations: Annotations): ClobValue

    abstract override fun withAnnotations(annotations: Annotations): ClobValue

    abstract override fun withoutAnnotations(): ClobValue
}

@PartiQLValueExperimental
public abstract class BinaryValue : ScalarValue<BitSet> {

    override val type: PartiQLValueType = PartiQLValueType.BINARY

    abstract override fun copy(annotations: Annotations): BinaryValue

    abstract override fun withAnnotations(annotations: Annotations): BinaryValue

    abstract override fun withoutAnnotations(): BinaryValue
}

@PartiQLValueExperimental
public abstract class ByteValue : ScalarValue<Byte> {

    override val type: PartiQLValueType = PartiQLValueType.BYTE

    abstract override fun copy(annotations: Annotations): ByteValue

    abstract override fun withAnnotations(annotations: Annotations): ByteValue

    abstract override fun withoutAnnotations(): ByteValue
}

@PartiQLValueExperimental
public abstract class BlobValue : ScalarValue<ByteArray> {

    override val type: PartiQLValueType = PartiQLValueType.BLOB

    abstract override fun copy(annotations: Annotations): BlobValue

    abstract override fun withAnnotations(annotations: Annotations): BlobValue

    abstract override fun withoutAnnotations(): BlobValue
}

@PartiQLValueExperimental
public abstract class DateValue : ScalarValue<Date> {

    override val type: PartiQLValueType = PartiQLValueType.DATE

    abstract override fun copy(annotations: Annotations): DateValue

    abstract override fun withAnnotations(annotations: Annotations): DateValue

    abstract override fun withoutAnnotations(): DateValue
}

@PartiQLValueExperimental
public abstract class TimeValue : ScalarValue<Time> {

    override val type: PartiQLValueType = PartiQLValueType.TIME

    abstract override fun copy(annotations: Annotations): TimeValue

    abstract override fun withAnnotations(annotations: Annotations): TimeValue

    abstract override fun withoutAnnotations(): TimeValue
}

@PartiQLValueExperimental
public abstract class TimestampValue : ScalarValue<Timestamp> {

    override val type: PartiQLValueType = PartiQLValueType.TIMESTAMP

    abstract override fun copy(annotations: Annotations): TimestampValue

    abstract override fun withAnnotations(annotations: Annotations): TimestampValue

    abstract override fun withoutAnnotations(): TimestampValue
}

@PartiQLValueExperimental
public abstract class IntervalValue : ScalarValue<Long> {

    override val type: PartiQLValueType = PartiQLValueType.INTERVAL

    abstract override fun copy(annotations: Annotations): IntervalValue

    abstract override fun withAnnotations(annotations: Annotations): IntervalValue

    abstract override fun withoutAnnotations(): IntervalValue
}

@PartiQLValueExperimental
public abstract class BagValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.BAG

    abstract override fun copy(annotations: Annotations): BagValue<T>

    abstract override fun withAnnotations(annotations: Annotations): BagValue<T>

    abstract override fun withoutAnnotations(): BagValue<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BagValue<*>
        if (other.annotations != this.annotations) return false

        // one (or both) null.bag
        if (this.isNull || other.isNull) return this.isNull == other.isNull

        // both not null, compare values
        val lhs = this.toList()
        val rhs = other.toList()
        // this is incorrect as it assumes ordered-ness, but we don't have a sort or hash yet
        return lhs == rhs
    }

    override fun hashCode(): Int {
        // TODO
        return type.hashCode()
    }
}

@PartiQLValueExperimental
public abstract class ListValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.LIST

    abstract override fun copy(annotations: Annotations): ListValue<T>

    abstract override fun withAnnotations(annotations: Annotations): ListValue<T>

    abstract override fun withoutAnnotations(): ListValue<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ListValue<*>
        if (other.annotations != this.annotations) return false

        // one (or both) null.list
        if (this.isNull || other.isNull) return this.isNull == other.isNull

        // both not null, compare values
        val lhs = this.toList()
        val rhs = other.toList()
        return lhs == rhs
    }

    override fun hashCode(): Int {
        // TODO
        return type.hashCode()
    }
}

@PartiQLValueExperimental
public abstract class SexpValue<T : PartiQLValue> : CollectionValue<T> {

    override val type: PartiQLValueType = PartiQLValueType.SEXP

    abstract override fun copy(annotations: Annotations): SexpValue<T>

    abstract override fun withAnnotations(annotations: Annotations): SexpValue<T>

    abstract override fun withoutAnnotations(): SexpValue<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SexpValue<*>) return false
        if (other.annotations != this.annotations) return false

        // one (or both) null.sexp
        if (this.isNull || other.isNull) return this.isNull == other.isNull

        // both not null, compare values
        val lhs = this.toList()
        val rhs = other.toList()
        return lhs == rhs
    }

    override fun hashCode(): Int {
        // TODO
        return type.hashCode()
    }
}

@PartiQLValueExperimental
public abstract class StructValue<T : PartiQLValue> : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.STRUCT

    public abstract val fields: Iterable<String>

    public abstract val values: Iterable<T>

    public abstract val entries: Iterable<Pair<String, T>>

    public abstract operator fun get(key: String): T?

    public abstract fun getAll(key: String): Iterable<T>

    abstract override fun copy(annotations: Annotations): StructValue<T>

    abstract override fun withAnnotations(annotations: Annotations): StructValue<T>

    abstract override fun withoutAnnotations(): StructValue<T>

    /**
     * Checks equality of struct entries, ignoring ordering.
     *
     * @param other
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StructValue<*>) return false
        if (other.annotations != this.annotations) return false

        // one (or both) null.struct
        if (this.isNull || other.isNull) return this.isNull == other.isNull

        // both not null, compare fields
        val lhs = this.entries.asIterable().groupBy({ it.first }, { it.second })
        val rhs = other.entries.asIterable().groupBy({ it.first }, { it.second })

        // check size
        if (lhs.size != rhs.size) return false
        if (lhs.keys != rhs.keys) return false

        // check values
        lhs.entries.forEach { (key, values) ->
            val lGroup: Map<PartiQLValue, Int> = values.groupingBy { it }.eachCount()
            val rGroup: Map<PartiQLValue, Int> = rhs[key]!!.groupingBy { it }.eachCount()
            if (lGroup != rGroup) return false
        }
        return true
    }

    override fun hashCode(): Int {
        // TODO
        return entries.hashCode()
    }

    override fun toString(): String {
        if (isNull) {
            return "null"
        }
        return super.toString()
    }
}

@PartiQLValueExperimental
public abstract class NullValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.NULL

    override val isNull: Boolean = true

    public abstract fun withType(type: PartiQLValueType): PartiQLValue

    abstract override fun copy(annotations: Annotations): NullValue

    abstract override fun withAnnotations(annotations: Annotations): NullValue

    abstract override fun withoutAnnotations(): NullValue
}

@PartiQLValueExperimental
public abstract class MissingValue : PartiQLValue {

    override val type: PartiQLValueType = PartiQLValueType.MISSING

    override val isNull: Boolean = false

    abstract override fun copy(annotations: Annotations): MissingValue

    abstract override fun withAnnotations(annotations: Annotations): MissingValue

    abstract override fun withoutAnnotations(): MissingValue
}

@PartiQLValueExperimental
public fun PartiQLValue.toIon(): IonElement = accept(ToIon, Unit)

@PartiQLValueExperimental
@Throws(TypeCheckException::class)
public inline fun <reified T : PartiQLValue> PartiQLValue.check(): T {
    if (this is T) return this else throw TypeCheckException()
}
