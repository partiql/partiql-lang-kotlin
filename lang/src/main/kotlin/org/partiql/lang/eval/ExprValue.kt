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
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonValue
import com.amazon.ion.facet.Faceted
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.bytesValue
import java.math.BigDecimal

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
        @JvmStatic
        fun of(value: IonValue): ExprValue {
            val valueFactory = ExprValueFactory.standard(value.system)
            return when {
                value.isNullValue && value.hasTypeAnnotation(MISSING_ANNOTATION) -> valueFactory.missingValue // MISSING
                value.isNullValue -> NullExprValue(value.type) // NULL
                value is IonBool -> valueFactory.newBoolean(value.booleanValue()) // BOOL
                value is IonInt -> valueFactory.newInt(value.longValue()) // INT
                value is IonFloat -> valueFactory.newFloat(value.doubleValue()) // FLOAT
                value is IonDecimal -> valueFactory.newDecimal(value.decimalValue()) // DECIMAL
                value is IonTimestamp && value.hasTypeAnnotation(DATE_ANNOTATION) -> {
                    val timestampValue = value.timestampValue()
                    valueFactory.newDate(timestampValue.year, timestampValue.month, timestampValue.day)
                } // DATE
                value is IonTimestamp -> valueFactory.newTimestamp(value.timestampValue()) // TIMESTAMP
                value is IonStruct && value.hasTypeAnnotation(TIME_ANNOTATION) -> {
                    val hourValue = (value["hour"] as IonInt).intValue()
                    val minuteValue = (value["minute"] as IonInt).intValue()
                    val secondInDecimal = (value["second"] as IonDecimal).decimalValue()
                    val secondValue = secondInDecimal.toInt()
                    val nanoValue = secondInDecimal.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal()).toInt()
                    val timeZoneHourValue = (value["timezone_hour"] as IonInt).intValue()
                    val timeZoneMinuteValue = (value["timezone_minute"] as IonInt).intValue()
                    valueFactory.newTime(Time.of(hourValue, minuteValue, secondValue, nanoValue, secondInDecimal.scale(), timeZoneHourValue * 60 + timeZoneMinuteValue))
                } // TIME
                value is IonSymbol -> valueFactory.newSymbol(value.stringValue()) // SYMBOL
                value is IonString -> valueFactory.newString(value.stringValue()) // STRING
                value is IonClob -> valueFactory.newClob(value.bytesValue()) // CLOB
                value is IonBlob -> valueFactory.newBlob(value.bytesValue()) // BLOB
                value is IonList && value.hasTypeAnnotation(BAG_ANNOTATION) -> valueFactory.newBag(value.map { of(it) }) // BAG
                value is IonList -> valueFactory.newList(value.map { of(it) }) // LIST
                value is IonSexp -> valueFactory.newSexp(value.map { of(it) }) // SEXP
                value is IonStruct -> IonStructExprValue(valueFactory, value) // STRUCT
                else -> error("Unrecognized IonValue to transform to ExprValue: $value")
            }
        }
    }

    private class IonStructExprValue(
        valueFactory: ExprValueFactory,
        ionStruct: IonStruct
    ) : StructExprValue(
        StructOrdering.UNORDERED,
        ionStruct.asSequence().map {
            of(it).namedValue(valueFactory.newString(it.fieldName))
        }
    ) {
        override val bindings: Bindings<ExprValue> =
            IonStructBindings(valueFactory, ionStruct)
    }
}
