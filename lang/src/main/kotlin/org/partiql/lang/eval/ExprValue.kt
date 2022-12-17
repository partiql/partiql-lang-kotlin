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
        // Constructor functions
        private fun constructMissingValue() =
            object : BaseExprValue() {
                override val type = ExprValueType.MISSING
            }

        private fun constructNullValue(ionType: IonType = IonType.NULL) =
            object : BaseExprValue() {
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

        private fun constructBoolValue(value: Boolean) =
            object : ScalarExprValue() {
                override val type = ExprValueType.BOOL
                override fun booleanValue(): Boolean = value
            }

        private fun constructStringValue(value: String) =
            object : ScalarExprValue() {
                override val type = ExprValueType.STRING
                override fun stringValue() = value
            }

        private fun constructSymbolValue(value: String) =
            object : ScalarExprValue() {
                override val type = ExprValueType.SYMBOL
                override fun stringValue() = value
            }

        private fun constructIntValue(value: Long) =
            object : ScalarExprValue() {
                override val type = ExprValueType.INT
                override fun numberValue() = value
            }

        private fun constructFloatValue(value: Double) =
            object : ScalarExprValue() {
                override val type = ExprValueType.FLOAT
                override fun numberValue() = value
            }

        private fun constructDecimalValue(value: BigDecimal) =
            object : ScalarExprValue() {
                override val type = ExprValueType.DECIMAL
                override fun numberValue() = value
            }

        private fun constructDateValue(value: LocalDate): ExprValue {
            // validate that the local date is not an extended date.
            if (value.year < 0 || value.year > 9999) {
                err(
                    "Year should be in the range 0 to 9999 inclusive.",
                    ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                    propertyValueMapOf(),
                    false
                )
            }

            return object : ScalarExprValue() {
                override val type = ExprValueType.DATE
                override fun dateValue() = value
            }
        }

        private fun constructTimestampValue(value: Timestamp) =
            object : ScalarExprValue() {
                override val type = ExprValueType.TIMESTAMP
                override fun timestampValue() = value
            }

        private fun constructTimeValue(value: Time) =
            object : ScalarExprValue() {
                override val type = ExprValueType.TIME
                override fun timeValue() = value
            }

        private fun constructClobValue(value: ByteArray) =
            object : ScalarExprValue() {
                override val type = ExprValueType.CLOB
                override fun bytesValue() = value
            }

        private fun constructBlobValue(value: ByteArray) =
            object : ScalarExprValue() {
                override val type = ExprValueType.BLOB
                override fun bytesValue() = value
            }

        private fun constructListValue(values: Sequence<ExprValue>) =
            object : BaseExprValue() {
                override val type = ExprValueType.LIST
                override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
                override fun iterator() = values.mapIndexed { i, v -> v.namedValue(newInt(i)) }.iterator()
            }

        private fun constructBagValue(values: Sequence<ExprValue>) =
            object : BaseExprValue() {
                override val type = ExprValueType.BAG
                override val ordinalBindings = OrdinalBindings.EMPTY
                override fun iterator() = values.iterator()
            }

        private fun constructSexpValue(values: Sequence<ExprValue>) =
            object : BaseExprValue() {
                override val type = ExprValueType.SEXP
                override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
                override fun iterator() = values.mapIndexed { i, v -> v.namedValue(newInt(i)) }.iterator()
            }

        private fun constructStructValue(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
            StructExprValue(ordering, values)

        // Memoized values for optimization
        private val trueValue = constructBoolValue(true)
        private val falseValue = constructBoolValue(false)
        private val emptyString = constructStringValue("")
        private val emptySymbol = constructSymbolValue("")

        // Public API
        @JvmStatic
        val missingValue: ExprValue = constructMissingValue()

        @JvmStatic
        val nullValue: ExprValue = constructNullValue()

        @JvmStatic
        fun newNull(ionType: IonType): ExprValue =
            when (ionType) {
                IonType.NULL -> nullValue
                else -> constructNullValue(ionType)
            }

        @JvmStatic
        fun newBoolean(value: Boolean): ExprValue =
            when (value) {
                true -> trueValue
                false -> falseValue
            }

        @JvmStatic
        fun newString(value: String): ExprValue =
            when {
                value.isEmpty() -> emptyString
                else -> constructStringValue(value)
            }

        @JvmStatic
        fun newSymbol(value: String): ExprValue =
            when {
                value.isEmpty() -> emptySymbol
                else -> constructSymbolValue(value)
            }

        @JvmStatic
        fun newInt(value: Long): ExprValue =
            constructIntValue(value)

        @JvmStatic
        fun newInt(value: Int): ExprValue =
            constructIntValue(value.toLong())

        @JvmStatic
        fun newFloat(value: Double): ExprValue =
            constructFloatValue(value)

        @JvmStatic
        fun newDecimal(value: BigDecimal): ExprValue =
            constructDecimalValue(value)

        @JvmStatic
        fun newDecimal(value: Int): ExprValue =
            constructDecimalValue(BigDecimal.valueOf(value.toLong()))

        @JvmStatic
        fun newDecimal(value: Long): ExprValue =
            constructDecimalValue(BigDecimal.valueOf(value))

        @JvmStatic
        fun newDate(value: LocalDate): ExprValue =
            constructDateValue(value)

        @JvmStatic
        fun newDate(year: Int, month: Int, day: Int): ExprValue =
            constructDateValue(LocalDate.of(year, month, day))

        @JvmStatic
        fun newDate(value: String): ExprValue =
            constructDateValue(LocalDate.parse(value))

        @JvmStatic
        fun newTimestamp(value: Timestamp): ExprValue =
            constructTimestampValue(value)

        @JvmStatic
        fun newTime(value: Time): ExprValue =
            constructTimeValue(value)

        @JvmStatic
        fun newClob(value: ByteArray): ExprValue =
            constructClobValue(value)

        @JvmStatic
        fun newBlob(value: ByteArray): ExprValue =
            constructBlobValue(value)

        @JvmStatic
        fun newList(values: Sequence<ExprValue>): ExprValue =
            constructListValue(values)

        @JvmStatic
        fun newList(values: Iterable<ExprValue>): ExprValue =
            constructListValue(values.asSequence())

        @JvmStatic
        val emptyList = constructListValue(sequenceOf())

        @JvmStatic
        fun newBag(values: Sequence<ExprValue>): ExprValue =
            constructBagValue(values)

        @JvmStatic
        fun newBag(values: Iterable<ExprValue>): ExprValue =
            constructBagValue(values.asSequence())

        @JvmStatic
        val emptyBag = constructBagValue(sequenceOf())

        @JvmStatic
        fun newSexp(values: Sequence<ExprValue>): ExprValue =
            constructSexpValue(values)

        @JvmStatic
        fun newSexp(values: Iterable<ExprValue>): ExprValue =
            constructSexpValue(values.asSequence())

        @JvmStatic
        val emptySexp = constructSexpValue(sequenceOf())

        @JvmStatic
        fun newStruct(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
            constructStructValue(values, ordering)

        @JvmStatic
        fun newStruct(values: Iterable<ExprValue>, ordering: StructOrdering): ExprValue =
            newStruct(values.asSequence(), ordering)

        @JvmStatic
        val emptyStruct = constructStructValue(sequenceOf(), StructOrdering.UNORDERED)

        @JvmStatic
        fun newFromIonReader(ion: IonSystem, reader: IonReader): ExprValue =
            of(ion.newValue(reader))

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
