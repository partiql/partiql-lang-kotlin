/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonReader
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonSystem
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ion.facet.Faceted
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.bytesValue
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Representation of a value within the context of an [Expression].
 */
interface ExprValue : Iterable<ExprValue>, Faceted {
    /** The type of value independent of its implementation. */
    val type: ExprValueType

    /** Returns the [Scalar] view of this value. */
    val scalar: Scalar

    /**
     * Returns the [Bindings] over this value.
     *
     * This is generally used to access a component of a value by name.
     */
    // TODO: Improve ergonomics of this API
    val bindings: Bindings<ExprValue>

    /**
     * Returns the [OrdinalBindings] over this value.
     *
     * This is generally used to access a component of a value by index.
     */
    // TODO: Improve ergonomics of this API
    val ordinalBindings: OrdinalBindings

    /**
     * Iterates over this value's *child* elements.
     *
     * If this value has no children, then it should return the empty iterator.
     */
    override operator fun iterator(): Iterator<ExprValue>

    companion object {
        // Constructor classes
        private class NullExprValue(val ionType: IonType = IonType.NULL) : BaseExprValue() {
            override val type = ExprValueType.NULL

            @Suppress("UNCHECKED_CAST")
            override fun <T> provideFacet(type: Class<T>?): T? = when (type) {
                IonType::class.java -> ionType as T?
                else -> null
            }
        }

        private abstract class ScalarExprValue : BaseExprValue(), Scalar {
            override val scalar: Scalar
                get() = this
        }

        private class BooleanExprValue(val value: Boolean) : ScalarExprValue() {
            override val type = ExprValueType.BOOL
            override fun booleanValue(): Boolean = value
        }

        private class StringExprValue(val value: String) : ScalarExprValue() {
            override val type = ExprValueType.STRING
            override fun stringValue() = value
        }

        private class SymbolExprValue(val value: String) : ScalarExprValue() {
            override val type = ExprValueType.SYMBOL
            override fun stringValue() = value
        }

        private class IntExprValue(val value: Long) : ScalarExprValue() {
            override val type = ExprValueType.INT
            override fun numberValue() = value
        }

        private class FloatExprValue(val value: Double) : ScalarExprValue() {
            override val type = ExprValueType.FLOAT
            override fun numberValue() = value
        }

        private class DecimalExprValue(val value: BigDecimal) : ScalarExprValue() {
            override val type = ExprValueType.DECIMAL
            override fun numberValue() = value
        }

        private class DateExprValue(val value: LocalDate) : ScalarExprValue() {
            init {
                // validate that the local date is not an extended date.
                if (value.year < 0 || value.year > 9999) {
                    err(
                        "Year should be in the range 0 to 9999 inclusive.",
                        ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                        propertyValueMapOf(),
                        false
                    )
                }
            }

            override val type: ExprValueType = ExprValueType.DATE
            override fun dateValue(): LocalDate = value
        }

        private class TimestampExprValue(val value: Timestamp) : ScalarExprValue() {
            override val type: ExprValueType = ExprValueType.TIMESTAMP
            override fun timestampValue(): Timestamp = value
        }

        private class TimeExprValue(val value: Time) : ScalarExprValue() {
            override val type = ExprValueType.TIME
            override fun timeValue(): Time = value
        }

        private class ClobExprValue(val value: ByteArray) : ScalarExprValue() {
            override val type: ExprValueType = ExprValueType.CLOB
            override fun bytesValue() = value
        }

        private class BlobExprValue(val value: ByteArray) : ScalarExprValue() {
            override val type: ExprValueType = ExprValueType.BLOB
            override fun bytesValue() = value
        }

        private class ListExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
            override val type = ExprValueType.LIST
            override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
            override fun iterator() = values.mapIndexed { i, v -> v.namedValue(newInt(i)) }.iterator()
        }

        private class BagExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
            override val type = ExprValueType.BAG
            override val ordinalBindings = OrdinalBindings.EMPTY
            override fun iterator() = values.iterator()
        }

        private class SexpExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
            override val type = ExprValueType.SEXP
            override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
            override fun iterator() = values.mapIndexed { i, v -> v.namedValue(newInt(i)) }.iterator()
        }

        // Memoized values for optimization
        private val trueValue = BooleanExprValue(true)
        private val falseValue = BooleanExprValue(false)
        private val emptyString = StringExprValue("")
        private val emptySymbol = SymbolExprValue("")

        // Public API

        /** A possibly memoized, immutable [ExprValue] representing the PartiQL missing value. */
        @JvmStatic
        val missingValue: ExprValue = object : BaseExprValue() {
            override val type = ExprValueType.MISSING
        }

        /** A possibly memoized, immutable [ExprValue] representing the PartiQL null value. */
        @JvmStatic
        val nullValue: ExprValue = NullExprValue()

        @JvmStatic
        fun newNull(ionType: IonType): ExprValue =
            when (ionType) {
                IonType.NULL -> nullValue
                else -> NullExprValue(ionType)
            }

        /** Returns a possibly memoized instance of [ExprValue] representing the specified [Boolean]. */
        @JvmStatic
        fun newBoolean(value: Boolean): ExprValue =
            when (value) {
                true -> trueValue
                false -> falseValue
            }

        /** Returns a possibly memoized [ExprValue] instance representing the specified [String]. */
        @JvmStatic
        fun newString(value: String): ExprValue =
            when {
                value.isEmpty() -> emptyString
                else -> StringExprValue(value)
            }

        /** Returns an  PartiQL `SYMBOL` [ExprValue] instance representing the specified [String]. */
        @JvmStatic
        fun newSymbol(value: String): ExprValue =
            when {
                value.isEmpty() -> emptySymbol
                else -> SymbolExprValue(value)
            }

        /** Returns a PartiQL `INT` [ExprValue] instance representing the specified [Long]. */
        @JvmStatic
        fun newInt(value: Long): ExprValue =
            IntExprValue(value)

        /** Returns a PartiQL `INT` ][ExprValue] instance representing the specified [Int]. */
        @JvmStatic
        fun newInt(value: Int): ExprValue =
            IntExprValue(value.toLong())

        /** Returns a PartiQL `FLOAT` [ExprValue] instance representing the specified [Float]. */
        @JvmStatic
        fun newFloat(value: Double): ExprValue =
            FloatExprValue(value)

        /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [BigDecimal]. */
        @JvmStatic
        fun newDecimal(value: BigDecimal): ExprValue =
            DecimalExprValue(value)

        /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [Int]. */
        @JvmStatic
        fun newDecimal(value: Int): ExprValue =
            DecimalExprValue(BigDecimal.valueOf(value.toLong()))

        /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [Long]. */
        @JvmStatic
        fun newDecimal(value: Long): ExprValue =
            DecimalExprValue(BigDecimal.valueOf(value))

        /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified [LocalDate]. */
        @JvmStatic
        fun newDate(value: LocalDate): ExprValue =
            DateExprValue(value)

        /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified year, month and day. */
        @JvmStatic
        fun newDate(year: Int, month: Int, day: Int): ExprValue =
            DateExprValue(LocalDate.of(year, month, day))

        /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified date string of the format yyyy-MM-dd. */
        @JvmStatic
        fun newDate(value: String): ExprValue =
            DateExprValue(LocalDate.parse(value))

        /** Returns a PartiQL `TIMESTAMP` [ExprValue] instance representing the specified [Timestamp]. */
        @JvmStatic
        fun newTimestamp(value: Timestamp): ExprValue =
            TimestampExprValue(value)

        /** Returns a PartiQL `TIME` [ExprValue] instance representing the specified [Time]. */
        @JvmStatic
        fun newTime(value: Time): ExprValue =
            TimeExprValue(value)

        /** Returns a PartiQL `CLOB` [ExprValue] instance representing the specified [ByteArray]. */
        @JvmStatic
        fun newClob(value: ByteArray): ExprValue =
            ClobExprValue(value)

        /** Returns a PartiQL `BLOB` [ExprValue] instance representing the specified [ByteArray]. */
        @JvmStatic
        fun newBlob(value: ByteArray): ExprValue =
            BlobExprValue(value)

        /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `LIST`. */
        @JvmStatic
        fun newList(values: Sequence<ExprValue>): ExprValue =
            ListExprValue(values)

        /** See newList(Sequence) */
        @JvmStatic
        fun newList(values: Iterable<ExprValue>): ExprValue =
            ListExprValue(values.asSequence())

        /** A possibly memoized, immutable [ExprValue] representing an empty list. */
        @JvmStatic
        val emptyList: ExprValue = ListExprValue(sequenceOf())

        /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `BAG`. */
        @JvmStatic
        fun newBag(values: Sequence<ExprValue>): ExprValue =
            BagExprValue(values)

        /** See newBag(Sequence) */
        @JvmStatic
        fun newBag(values: Iterable<ExprValue>): ExprValue =
            BagExprValue(values.asSequence())

        /** A possibly memoized, immutable [ExprValue] representing an empty bag. */
        @JvmStatic
        val emptyBag: ExprValue = BagExprValue(sequenceOf())

        /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `SEXP`. */
        @JvmStatic
        fun newSexp(values: Sequence<ExprValue>): ExprValue =
            SexpExprValue(values)

        /** See newSexp(Sequence) */
        @JvmStatic
        fun newSexp(values: Iterable<ExprValue>): ExprValue =
            SexpExprValue(values.asSequence())

        /** A possibly memoized, immutable [ExprValue] representing an empty sexp. */
        @JvmStatic
        val emptySexp: ExprValue = SexpExprValue(sequenceOf())

        /**
         * Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `STRUCT`.
         * The [ExprValue] instances within [values] should be [Named].
         *
         * [ordering] specifies if the field order is to be preserved or not.
         */
        @JvmStatic
        fun newStruct(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
            StructExprValue(ordering, values)

        /** See newStruct(Sequence) */
        @JvmStatic
        fun newStruct(values: Iterable<ExprValue>, ordering: StructOrdering): ExprValue =
            StructExprValue(ordering, values.asSequence())

        /** A possibly memoized, immutable [ExprValue] representing an empty struct. */
        @JvmStatic
        val emptyStruct: ExprValue = StructExprValue(StructOrdering.UNORDERED, sequenceOf())

        /**
         * Creates a new [ExprValue] instance from the next value available from the specified [IonReader].
         *
         * Implementations should not close the [IonReader].
         */
        @JvmStatic
        fun newFromIonReader(ion: IonSystem, reader: IonReader): ExprValue =
            of(ion.newValue(reader))

        /**
         * Creates a new [ExprValue] instance from any Ion value.
         *
         * If possible, prefer the use of the other methods instead because they might return [ExprValue] instances
         * that are better optimized for their specific data type (depending on implementation).
         */
        @JvmStatic
        fun of(value: IonValue): ExprValue {
            return when {
                value.isNullValue && value.hasTypeAnnotation(MISSING_ANNOTATION) -> missingValue // MISSING
                value.isNullValue -> newNull(value.type) // NULL
                value is IonBool -> newBoolean(value.booleanValue()) // BOOL
                value is IonInt -> newInt(value.longValue()) // INT
                value is IonFloat -> newFloat(value.doubleValue()) // FLOAT
                value is IonDecimal -> newDecimal(value.decimalValue()) // DECIMAL
                value is IonTimestamp && value.hasTypeAnnotation(DATE_ANNOTATION) -> {
                    val timestampValue = value.timestampValue()
                    newDate(timestampValue.year, timestampValue.month, timestampValue.day)
                } // DATE
                value is IonTimestamp -> newTimestamp(value.timestampValue()) // TIMESTAMP
                value is IonStruct && value.hasTypeAnnotation(TIME_ANNOTATION) -> {
                    val hourValue = (value["hour"] as IonInt).intValue()
                    val minuteValue = (value["minute"] as IonInt).intValue()
                    val secondInDecimal = (value["second"] as IonDecimal).decimalValue()
                    val secondValue = secondInDecimal.toInt()
                    val nanoValue = secondInDecimal.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal()).toInt()
                    val timeZoneHourValue = (value["timezone_hour"] as IonInt).intValue()
                    val timeZoneMinuteValue = (value["timezone_minute"] as IonInt).intValue()
                    newTime(Time.of(hourValue, minuteValue, secondValue, nanoValue, secondInDecimal.scale(), timeZoneHourValue * 60 + timeZoneMinuteValue))
                } // TIME
                value is IonSymbol -> newSymbol(value.stringValue()) // SYMBOL
                value is IonString -> newString(value.stringValue()) // STRING
                value is IonClob -> newClob(value.bytesValue()) // CLOB
                value is IonBlob -> newBlob(value.bytesValue()) // BLOB
                value is IonList && value.hasTypeAnnotation(BAG_ANNOTATION) -> newBag(value.map { of(it) }) // BAG
                value is IonList -> newList(value.map { of(it) }) // LIST
                value is IonSexp -> newSexp(value.map { of(it) }) // SEXP
                value is IonStruct -> IonStructExprValue(value) // STRUCT
                else -> error("Unrecognized IonValue to transform to ExprValue: $value")
            }
        }
    }

    private class IonStructExprValue(
        ionStruct: IonStruct
    ) : StructExprValue(
        StructOrdering.UNORDERED,
        ionStruct.asSequence().map { of(it).namedValue(newString(it.fieldName)) }
    ) {
        override val bindings: Bindings<ExprValue> =
            IonStructBindings(ionStruct)
    }
}
