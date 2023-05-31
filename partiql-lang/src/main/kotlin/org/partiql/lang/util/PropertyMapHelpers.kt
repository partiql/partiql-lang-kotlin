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

@file:Suppress("DEPRECATION")

package org.partiql.lang.util

import com.amazon.ion.IonValue
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap

/**
 * Helper function to reduce the syntactical overhead of creating a [PropertyValueMap].
 */
fun propertyValueMapOf(vararg properties: Pair<Property, Any>): PropertyValueMap {
    val pvm = PropertyValueMap()
    properties.forEach {
        if (pvm.hasProperty(it.first)) throw IllegalArgumentException("Duplicate property: ${it.first.propertyName}")
        when (it.second) {
            is Int -> pvm[it.first] = it.second as Int
            is Long -> pvm[it.first] = it.second as Long
            is String -> pvm[it.first] = it.second as String
            is IonValue -> pvm[it.first] = it.second as IonValue
            is Enum<*> -> pvm[it.first] = it.second.toString()
            else -> throw IllegalArgumentException("Cannot convert ${it.second.javaClass.name} to PropertyValue")
        }
    }

    return pvm
}

/**
 * Helper function to reduce the syntactical overhead of creating a [PropertyValueMap].
 *
 * This overload accepts [line] and [column] arguments before other properties.
 */
fun propertyValueMapOf(
    line: Int,
    column: Int,
    vararg otherProperties: Pair<Property, Any>
): PropertyValueMap =
    propertyValueMapOf(
        *otherProperties,
        Property.LINE_NUMBER to line.toLong(),
        Property.COLUMN_NUMBER to column.toLong()
    )

/**
 * Simple overloaded infix operator which accepts a [Propery] and value as arguments and returns a [Pair].
 * Intended to be used in conjunction with [propertyValueMapOf].
 */
infix fun Property.to(that: Any): Pair<Property, Any> = Pair(this, that)
