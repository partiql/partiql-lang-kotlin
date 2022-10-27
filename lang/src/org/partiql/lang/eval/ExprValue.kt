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

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ion.facet.Faceted
import org.partiql.lang.util.seal

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
    val bindings: Bindings<ExprValue>

    /**
     * Returns the [OrdinalBindings] over this value.
     *
     * This is generally used to access a component of a value by index.
     */
    val ordinalBindings: OrdinalBindings

    /**
     * Iterates over this value's *child* elements.
     *
     * If this value has no children, then it should return the empty iterator.
     */
    override operator fun iterator(): Iterator<ExprValue>
}

fun ExprValue.toIonValue(ion: IonSystem): IonValue =
    when (type) {
        ExprValueType.NULL -> ion.newNull()
        ExprValueType.MISSING -> ion.newNull().apply { addTypeAnnotation(MISSING_ANNOTATION) }
        ExprValueType.BOOL -> ion.newBool(booleanValue())
        ExprValueType.INT -> ion.newInt(longValue())
        ExprValueType.FLOAT -> ion.newFloat(numberValue().toDouble())
        ExprValueType.DECIMAL -> ion.newDecimal(bigDecimalValue())
        ExprValueType.DATE -> {
            val value = dateValue()
            ion.newTimestamp(Timestamp.forDay(value.year, value.monthValue, value.dayOfMonth)).apply {
                addTypeAnnotation(DATE_ANNOTATION)
            }.seal()
        }
        ExprValueType.TIMESTAMP -> ion.newTimestamp(timestampValue())
        ExprValueType.TIME -> timeValue().toIonValue(ion)
        ExprValueType.SYMBOL -> ion.newSymbol(stringValue())
        ExprValueType.STRING -> ion.newString(stringValue())
        ExprValueType.CLOB -> ion.newClob(bytesValue())
        ExprValueType.BLOB -> ion.newBlob(bytesValue())
        ExprValueType.LIST -> mapTo(ion.newEmptyList()) {
            it.toIonValue(ion).clone()
        }
        ExprValueType.SEXP -> mapTo(ion.newEmptySexp()) {
            it.toIonValue(ion).clone()
        }
        ExprValueType.STRUCT -> toIonStruct(ion)
        ExprValueType.BAG -> mapTo(
            ion.newEmptyList().apply { addTypeAnnotation(BAG_ANNOTATION) }
        ) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
    }.seal()

/**
 * [SequenceExprValue] may call this function to get a mutable instance of the IonValue that it can add
 * directly to its lazily constructed list.  This is a performance optimization--otherwise the value would be
 * cloned twice: once by this class's implementation of [ionValue], and again before adding the value to its list.
 *
 * Note: it is not possible to add a sealed (non-mutuable) [IonValue] as a child of a container.
 */
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
