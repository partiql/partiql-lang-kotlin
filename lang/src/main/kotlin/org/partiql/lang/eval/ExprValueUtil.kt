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

import com.amazon.ion.IonReader
import com.amazon.ion.IonSystem
import com.amazon.ion.IonType
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.collections.asSequence

const val MISSING_ANNOTATION = "\$missing"
const val BAG_ANNOTATION = "\$bag"
const val DATE_ANNOTATION = "\$date"
const val TIME_ANNOTATION = "\$time"

// TODO: remove this when we remvoe [ExprValue.ionValue]
private val ion = IonSystemBuilder.standard().build()

// Constructor functions
private fun constructMissingValue() =
    object : BaseExprValue() {
        override val type = ExprValueType.MISSING
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructNullValue(ionType: IonType = IonType.NULL) =
    object : BaseExprValue() {
        override val type = ExprValueType.NULL
        override val ionValue by lazy { toIonValue(ion) }

        @Suppress("UNCHECKED_CAST")
        override fun <T> provideFacet(type: Class<T>?): T? = when (type) {
            IonType::class.java -> ionType as T?
            else -> null
        }
    }

private fun constructBoolValue(value: Boolean) =
    object : BaseExprValue() {
        override val type = ExprValueType.BOOL
        override val scalar = object : Scalar {
            override fun booleanValue(): Boolean = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructStringValue(value: String) =
    object : BaseExprValue() {
        override val type = ExprValueType.STRING
        override val scalar = object : Scalar {
            override fun stringValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructSymbolValue(value: String) =
    object : BaseExprValue() {
        override val type = ExprValueType.SYMBOL
        override val scalar = object : Scalar {
            override fun stringValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructIntValue(value: Long) =
    object : BaseExprValue() {
        override val type = ExprValueType.INT
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructFloatValue(value: Double) =
    object : BaseExprValue() {
        override val type = ExprValueType.FLOAT
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructDecimalValue(value: BigDecimal) =
    object : BaseExprValue() {
        override val type = ExprValueType.DECIMAL
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
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

    return object : BaseExprValue() {
        override val type = ExprValueType.DATE
        override val scalar = object : Scalar {
            override fun dateValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }
}

private fun constructTimestampValue(value: Timestamp) =
    object : BaseExprValue() {
        override val type = ExprValueType.TIMESTAMP
        override val scalar = object : Scalar {
            override fun timestampValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructTimeValue(value: Time) =
    object : BaseExprValue() {
        override val type = ExprValueType.TIME
        override val scalar = object : Scalar {
            override fun timeValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructClobValue(value: ByteArray) =
    object : BaseExprValue() {
        override val type = ExprValueType.CLOB
        override val scalar = object : Scalar {
            override fun bytesValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructBlobValue(value: ByteArray) =
    object : BaseExprValue() {
        override val type = ExprValueType.BLOB
        override val scalar = object : Scalar {
            override fun bytesValue() = value
        }
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructListValue(values: Sequence<ExprValue>) =
    object : BaseExprValue() {
        override val type = ExprValueType.LIST
        override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
        override fun iterator() = values.mapIndexed { i, v -> v.namedValue(exprInt(i)) }.iterator()
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructBagValue(values: Sequence<ExprValue>) =
    object : BaseExprValue() {
        override val type = ExprValueType.BAG
        override val ordinalBindings = OrdinalBindings.EMPTY
        override fun iterator() = values.iterator()
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructSexpValue(values: Sequence<ExprValue>) =
    object : BaseExprValue() {
        override val type = ExprValueType.SEXP
        override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
        override fun iterator() = values.mapIndexed { i, v -> v.namedValue(exprInt(i)) }.iterator()
        override val ionValue by lazy { toIonValue(ion) }
    }

private fun constructStructValue(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
    StructExprValue(ion, ordering, values)

// States for optimization
private val missingValue = constructMissingValue()
private val nullValue = constructNullValue()
private val trueValue = constructBoolValue(true)
private val falseValue = constructBoolValue(false)
private val emptyString = constructStringValue("")
private val emptySymbol = constructSymbolValue("")
private val emptyList = constructListValue(sequenceOf())
private val emptyBag = constructBagValue(sequenceOf())
private val emptySexp = constructSexpValue(sequenceOf())
private val emptyStruct = constructStructValue(sequenceOf(), StructOrdering.UNORDERED)

// Public API
fun exprMissing(): ExprValue =
    missingValue

fun exprNull(ionType: IonType = IonType.NULL): ExprValue =
    when (ionType) {
        IonType.NULL -> nullValue
        else -> constructNullValue(ionType)
    }

fun exprBoolean(value: Boolean): ExprValue =
    when (value) {
        true -> trueValue
        false -> falseValue
    }

fun exprString(value: String): ExprValue =
    when {
        value.isEmpty() -> emptyString
        else -> constructStringValue(value)
    }

fun exprSymbol(value: String): ExprValue =
    when {
        value.isEmpty() -> emptySymbol
        else -> constructSymbolValue(value)
    }

fun exprInt(value: Long): ExprValue =
    constructIntValue(value)

fun exprInt(value: Int): ExprValue =
    constructIntValue(value.toLong())

fun exprFloat(value: Double): ExprValue =
    constructFloatValue(value)

fun exprDecimal(value: BigDecimal): ExprValue =
    constructDecimalValue(value)

fun exprDecimal(value: Int): ExprValue =
    constructDecimalValue(BigDecimal.valueOf(value.toLong()))

fun exprDecimal(value: Long): ExprValue =
    constructDecimalValue(BigDecimal.valueOf(value))

fun exprDate(value: LocalDate): ExprValue =
    constructDateValue(value)

fun exprDate(year: Int, month: Int, day: Int): ExprValue =
    constructDateValue(LocalDate.of(year, month, day))

fun exprDate(value: String): ExprValue =
    constructDateValue(LocalDate.parse(value))

fun exprTimestamp(value: Timestamp): ExprValue =
    constructTimestampValue(value)

fun exprTime(value: Time): ExprValue =
    constructTimeValue(value)

fun exprClob(value: ByteArray): ExprValue =
    constructClobValue(value)

fun exprBlob(value: ByteArray): ExprValue =
    constructBlobValue(value)

fun exprList(values: Sequence<ExprValue>): ExprValue =
    constructListValue(values)

fun exprList(values: Iterable<ExprValue>): ExprValue =
    constructListValue(values.asSequence())

fun emptyExprList(): ExprValue =
    emptyList

fun exprBag(values: Sequence<ExprValue>): ExprValue =
    constructBagValue(values)

fun exprBag(values: Iterable<ExprValue>): ExprValue =
    constructBagValue(values.asSequence())

fun emptyExprBag(): ExprValue =
    emptyBag

fun exprSexp(values: Sequence<ExprValue>): ExprValue =
    constructSexpValue(values)

fun exprSexp(values: Iterable<ExprValue>): ExprValue =
    constructSexpValue(values.asSequence())

fun emptyExprSexp(): ExprValue =
    emptySexp

fun exprStruct(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
    constructStructValue(values, ordering)

fun exprStruct(values: Iterable<ExprValue>, ordering: StructOrdering): ExprValue =
    exprStruct(values.asSequence(), ordering)

fun emptyExprStruct(): ExprValue =
    emptyStruct

fun exprValue(ion: IonSystem, reader: IonReader): ExprValue =
    ion.newValue(reader).toExprValue()
