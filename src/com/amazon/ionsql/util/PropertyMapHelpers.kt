package com.amazon.ionsql.util

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.syntax.*

/**
 * Helper function to reduce the syntactical overhead of creating a [PropertyValueMap].
 */
fun propertyValueMapOf(vararg properties: Pair<Property, Any>): PropertyValueMap {
    val pvm = PropertyValueMap()
    properties.forEach {
        if (pvm.hasProperty(it.first)) throw IllegalArgumentException("Duplicate property: ${it.first.propertyName}")
        when (it.second) {
            is Int       -> pvm[it.first] = it.second as Int
            is Long      -> pvm[it.first] = it.second as Long
            is String    -> pvm[it.first] = it.second as String
            is TokenType -> pvm[it.first] = it.second as TokenType
            is IonValue  -> pvm[it.first] = it.second as IonValue
            is Enum<*>   -> pvm[it.first] = it.second.toString()
            else         -> throw IllegalArgumentException("Cannot convert ${it.second.javaClass.name} to PropertyValue")
        }
    }

    return pvm
}

/**
 * Simple overloaded infix operator which accepts a [Propery] and value as arguments and returns a [Pair].
 * Intended to be used in conjunction with [propertyValueMapOf].
 */
infix fun Property.to(that: Any): Pair<Property, Any> = Pair(this, that)

