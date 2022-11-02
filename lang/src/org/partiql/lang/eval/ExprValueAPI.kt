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

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.collections.asSequence

internal const val MISSING_ANNOTATION = "\$partiql_missing"
internal const val BAG_ANNOTATION = "\$partiql_bag"
internal const val DATE_ANNOTATION = "\$partiql_date"
internal const val TIME_ANNOTATION = "\$partiql_time"

// Constructor functions
private fun constructMissingValue() =
    object : BaseExprValue() {
        override val type = ExprValueType.MISSING
    }

private fun constructNullValue() =
    object : BaseExprValue() {
        override val type = ExprValueType.NULL
    }

private fun constructBoolValue(value: Boolean) =
    object : BaseExprValue() {
        override val type = ExprValueType.BOOL
        override val scalar = object : Scalar {
            override fun booleanValue(): Boolean = value
        }
    }

private fun constructStringValue(value: String) =
    object : BaseExprValue() {
        override val type = ExprValueType.STRING
        override val scalar = object : Scalar {
            override fun stringValue() = value
        }
    }

private fun constructSymbolValue(value: String) =
    object : BaseExprValue() {
        override val type = ExprValueType.SYMBOL
        override val scalar = object : Scalar {
            override fun stringValue() = value
        }
    }

private fun constructIntValue(value: Long) =
    object : BaseExprValue() {
        override val type = ExprValueType.INT
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
    }

private fun constructFloatValue(value: Double) =
    object : BaseExprValue() {
        override val type = ExprValueType.FLOAT
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
    }

private fun constructDecimalValue(value: BigDecimal) =
    object : BaseExprValue() {
        override val type = ExprValueType.DECIMAL
        override val scalar = object : Scalar {
            override fun numberValue() = value
        }
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
    }
}

private fun constructTimestampValue(value: Timestamp) =
    object : BaseExprValue() {
        override val type = ExprValueType.TIMESTAMP
        override val scalar = object : Scalar {
            override fun timestampValue() = value
        }
    }

private fun constructTimeValue(value: Time) =
    object : BaseExprValue() {
        override val type = ExprValueType.TIME
        override val scalar = object : Scalar {
            override fun timeValue() = value
        }
    }

private fun constructClobValue(value: ByteArray) =
    object : BaseExprValue() {
        override val type = ExprValueType.CLOB
        override val scalar = object : Scalar {
            override fun bytesValue() = value
        }
    }

private fun constructBlobValue(value: ByteArray) =
    object : BaseExprValue() {
        override val type = ExprValueType.BLOB
        override val scalar = object : Scalar {
            override fun bytesValue() = value
        }
    }

private fun constructListValue(values: Sequence<ExprValue>) =
    object : BaseExprValue() {
        override val type = ExprValueType.LIST
        override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
        override fun iterator() = values.mapIndexed { i, v -> v.namedValue(intExprValue(i)) }.iterator()
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
        override fun iterator() = values.mapIndexed { i, v -> v.namedValue(intExprValue(i)) }.iterator()
    }

private fun constructStructValue(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
    StructExprValue(ordering, values)

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
fun missingExprValue(): ExprValue =
    missingValue

fun nullExprValue(): ExprValue =
    nullValue

fun boolExprValue(value: Boolean): ExprValue =
    when (value) {
        true -> trueValue
        false -> falseValue
    }

fun stringExprValue(value: String): ExprValue =
    when {
        value.isEmpty() -> emptyString
        else -> constructStringValue(value)
    }

fun symbolExprValue(value: String): ExprValue =
    when {
        value.isEmpty() -> emptySymbol
        else -> constructSymbolValue(value)
    }

fun intExprValue(value: Long): ExprValue =
    constructIntValue(value)

fun intExprValue(value: Int): ExprValue =
    constructIntValue(value.toLong())

fun floatExprValue(value: Double): ExprValue =
    constructFloatValue(value)

fun decimalExprValue(value: BigDecimal): ExprValue =
    constructDecimalValue(value)

fun decimalExprValue(value: Int): ExprValue =
    constructDecimalValue(BigDecimal.valueOf(value.toLong()))

fun decimalExprValue(value: Long): ExprValue =
    constructDecimalValue(BigDecimal.valueOf(value))

fun dateExprValue(value: LocalDate): ExprValue =
    constructDateValue(value)

fun dateExprValue(year: Int, month: Int, day: Int): ExprValue =
    constructDateValue(LocalDate.of(year, month, day))

fun dateExprValue(value: String): ExprValue =
    constructDateValue(LocalDate.parse(value))

fun timestampExprValue(value: Timestamp): ExprValue =
    constructTimestampValue(value)

fun timeExprValue(value: Time): ExprValue =
    constructTimeValue(value)

fun clobExprValue(value: ByteArray): ExprValue =
    constructClobValue(value)

fun blobExprValue(value: ByteArray): ExprValue =
    constructBlobValue(value)

fun listExprValue(values: Sequence<ExprValue>): ExprValue =
    constructListValue(values)

fun listExprValue(values: Iterable<ExprValue>): ExprValue =
    constructListValue(values.asSequence())

fun emptyListExprValue(): ExprValue =
    emptyList

fun bagExprValue(values: Sequence<ExprValue>): ExprValue =
    constructBagValue(values)

fun bagExprValue(values: Iterable<ExprValue>): ExprValue =
    constructBagValue(values.asSequence())

fun emptyBagExprValue(): ExprValue =
    emptyBag

fun sexpExprValue(values: Sequence<ExprValue>): ExprValue =
    constructSexpValue(values)

fun sexpExprValue(values: Iterable<ExprValue>): ExprValue =
    constructSexpValue(values.asSequence())

fun emptySexpExprValue(): ExprValue =
    emptySexp

fun structExprValue(values: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
    constructStructValue(values, ordering)

fun structExprValue(values: Iterable<ExprValue>, ordering: StructOrdering): ExprValue =
    structExprValue(values.asSequence(), ordering)

fun emptyStructExprValue(): ExprValue =
    emptyStruct
