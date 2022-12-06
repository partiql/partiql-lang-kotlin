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
import com.amazon.ion.IonSystem
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
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

    /**
     * Materializes the expression value as an [IonValue].
     *
     * The returned value may or may not be tethered to a container, so it is
     * the callers responsibility to deal with that accordingly (e.g. via `clone`).
     */
    @Deprecated("Please use [ExprValue.toIonValue()] to transform [ExprValue] to [IonValue]")
    val ionValue: IonValue

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
}

/**
 * This method should only be used in case we want to get result from querying an Ion file or an [IonValue]
 */
fun ExprValue.toIonValue(ion: IonSystem): IonValue =
    when (type) {
        ExprValueType.NULL -> ion.newNull(asFacet(IonType::class.java))
        ExprValueType.MISSING -> ion.newNull().apply { addTypeAnnotation(MISSING_ANNOTATION) }
        ExprValueType.BOOL -> ion.newBool(booleanValue())
        ExprValueType.INT -> ion.newInt(longValue())
        ExprValueType.FLOAT -> ion.newFloat(numberValue().toDouble())
        ExprValueType.DECIMAL -> ion.newDecimal(bigDecimalValue())
        ExprValueType.DATE -> {
            val value = dateValue()
            ion.newTimestamp(Timestamp.forDay(value.year, value.monthValue, value.dayOfMonth)).apply {
                addTypeAnnotation(DATE_ANNOTATION)
            }
        }
        ExprValueType.TIMESTAMP -> ion.newTimestamp(timestampValue())
        ExprValueType.TIME -> timeValue().toIonValue(ion)
        ExprValueType.SYMBOL -> ion.newSymbol(stringValue())
        ExprValueType.STRING -> ion.newString(stringValue())
        ExprValueType.CLOB -> ion.newClob(bytesValue())
        ExprValueType.BLOB -> ion.newBlob(bytesValue())
        ExprValueType.LIST -> mapTo(ion.newEmptyList()) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.SEXP -> mapTo(ion.newEmptySexp()) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.BAG -> mapTo(
            ion.newEmptyList().apply { addTypeAnnotation(BAG_ANNOTATION) }
        ) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.STRUCT -> toIonStruct(ion)
    }

private fun ExprValue.toIonStruct(ion: IonSystem): IonStruct {
    return ion.newEmptyStruct().apply {
        this@toIonStruct.forEach {
            val nameVal = it.name
            if (nameVal != null && nameVal.type.isText && it.type != ExprValueType.MISSING) {
                val name = nameVal.stringValue()
                add(name, it.toIonValue(ion).clone())
            }
        }
    }
}

fun IonValue.toExprValue(): ExprValue =
    when {
        isNullValue && hasTypeAnnotation(MISSING_ANNOTATION) -> exprMissing() // MISSING
        isNullValue -> exprNull(type) // NULL
        this is IonBool -> exprBoolean(booleanValue()) // BOOL
        this is IonInt -> exprInt(longValue()) // INT
        this is IonFloat -> exprFloat(doubleValue()) // FLOAT
        this is IonDecimal -> exprDecimal(decimalValue()) // DECIMAL
        this is IonTimestamp && hasTypeAnnotation(DATE_ANNOTATION) -> {
            val timestampValue = timestampValue()
            exprDate(timestampValue.year, timestampValue.month, timestampValue.day)
        } // DATE
        this is IonTimestamp -> exprTimestamp(timestampValue()) // TIMESTAMP
        this is IonStruct && hasTypeAnnotation(TIME_ANNOTATION) -> {
            val hourValue = (this["hour"] as IonInt).intValue()
            val minuteValue = (this["minute"] as IonInt).intValue()
            val secondInDecimal = (this["second"] as IonDecimal).decimalValue()
            val secondValue = secondInDecimal.toInt()
            val nanoValue = secondInDecimal.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal()).toInt()
            val timeZoneHourValue = (this["timezone_hour"] as IonInt).intValue()
            val timeZoneMinuteValue = (this["timezone_minute"] as IonInt).intValue()
            exprTime(Time.of(hourValue, minuteValue, secondValue, nanoValue, secondInDecimal.scale(), timeZoneHourValue * 60 + timeZoneMinuteValue))
        } // TIME
        this is IonSymbol -> exprSymbol(stringValue()) // SYMBOL
        this is IonString -> exprString(stringValue()) // STRING
        this is IonClob -> exprClob(bytesValue()) // CLOB
        this is IonBlob -> exprBlob(bytesValue()) // BLOB
        this is IonList && hasTypeAnnotation(BAG_ANNOTATION) -> exprBag(map { it.toExprValue() }) // BAG
        this is IonList -> exprList(map { it.toExprValue() }) // LIST
        this is IonSexp -> exprSexp(map { it.toExprValue() }) // SEXP
        this is IonStruct -> IonStructExprValue(this) // STRUCT
        else -> error("Unrecognized IonValue to transform to ExprValue: $this")
    }

private class IonStructExprValue(
    private val ionStruct: IonStruct
) : StructExprValue(
    ionStruct.system,
    StructOrdering.UNORDERED,
    ionStruct.asSequence().map {
        it.toExprValue().namedValue(exprString(it.fieldName))
    }
) {
    override val bindings: Bindings<ExprValue> =
        IonStructBindings(ionStruct)

    override val ionValue: IonValue
        get() = ionStruct
}
