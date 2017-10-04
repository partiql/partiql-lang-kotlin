/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.errors

import com.amazon.ion.IonValue
import com.amazon.ionsql.errors.PropertyType.*
import com.amazon.ionsql.syntax.TokenType
import java.util.*


internal const val UNKNOWN: String = "<UNKNOWN>"

/**
 * Categories for errors. Should map to stages in the Compiler and Evaluator.
 */
enum class ErrorCategory(val message: String) {
    LEXER ("Lexer Error"),
    PARSER ("Parser Error"),
    EVALUATOR ("Evaluator Error");

    override fun toString() = message
}



/** Each possible value that can be reported as part of an error has a
 * [Property]. [Property] is used as a key in [PropertyValueMap].
 *
 * Each property contains
 *   1. a string name and a [propertyType] of the values that this property can have,
 *   1. and a [PropertyType] the denotes the type that the property's value can have.
 *
 * @param propertyName string name (internal use)
 * @param propertyType [Class] of object's that this property can hold in a [PropertyValueMap]
 *
 */
enum class Property(val propertyName: String, val propertyType: PropertyType) {

    LINE_NUMBER("line_no", LONG_CLASS),
    COLUMN_NUMBER("column_no", LONG_CLASS),
    TOKEN_STRING("token_string", STRING_CLASS),
    CAST_TO("cast_to", STRING_CLASS),
    KEYWORD("keyword", STRING_CLASS),
    TOKEN_TYPE("token_type", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE("expected_token_type", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE_1_OF_2("expected_token_type_1_of_2", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE_2_OF_2("expected_token_type_2_of_2", TOKEN_CLASS),
    TOKEN_VALUE("token_value", ION_VALUE_CLASS),
    EXPECTED_ARITY_MIN("arity_min", INTEGER_CLASS),
    EXPECTED_ARITY_MAX("arity_max", INTEGER_CLASS),
    TIMESTAMP_FORMAT_PATTERN("timestamp_format_pattern", STRING_CLASS)
}

/**
 * A [PropertyValue] is the top level type for all values that appear as properties in error codes.
 * For each type of value that can be a [Property] there is a method to allow clients to obtain the
 * correctly typed value.
 */
abstract class PropertyValue(val type: PropertyType) {
    open fun stringValue(): String = throw IllegalArgumentException("Property value is of type $type and not String")
    open fun longValue(): Long = throw IllegalArgumentException("Property value is of type $type and not Long")
    open fun tokenTypeValue(): TokenType = throw IllegalArgumentException("Property value is of type $type and not TokenType")
    open fun integerValue(): Int = throw IllegalArgumentException("Property value is of type $type and not Integer")
    open fun ionValue(): IonValue = throw IllegalArgumentException("Property value is of type $type and not IonValue")
}


/**
 * A [PropertyType] is a top level type for all types of values that appear as properties in error codes.
 * Clients can access the type (as a `Class<*>`) of a property's value through [getType()].
 *
 */
enum class PropertyType(private val type: Class<*>){
    LONG_CLASS (Long::class.javaObjectType),
    STRING_CLASS(String::class.javaObjectType),
    INTEGER_CLASS(Int::class.javaObjectType),
    TOKEN_CLASS(TokenType::class.javaObjectType),
    ION_VALUE_CLASS(IonValue::class.javaObjectType);

    public fun getType() = type
}

/**
 * A typed map of properties used to capture an error context.
 *
 * At each error location and for the specific error code `ec`  the implementation of IonSql++
 *
 *  1. creates a new [PropertyValueMap]
 *  1. **attempts** to add to add values for **all** [Property] found in `ec.getProperties()` set
 *
 *  It may be the case that the implementation was not able to populate one of the [Property] of `ec.getProperties()`.
 *  In that case the [PropertyValueMap] will **not** contains a key-value pair for that [Property].
 *
 *  Absence of a key means that there is no information. Clients **should** test a [Property] for membership in [PropertyValueMap]
 *
 *
 */
class PropertyValueMap(private val map: EnumMap<Property, PropertyValue> = EnumMap(Property::class.java)) {


    /**
     * Given a [Property]  retrieve the value mapped to [p] in this map.
     *
     *
     * @param key to be retrieved from the map
     * @return the value stored in this [PropertyValueMap] as a [PropertyValue], `null` if key is not present
     *
     */
    operator fun get(key: Property): PropertyValue? = map[key]


    private fun <T> verifyTypeAndSet(prop: Property, expectedType: PropertyType, value : T,  pValue: PropertyValue) {
        if (prop.propertyType == expectedType) {
            map[prop] = pValue
        } else {
            throw IllegalArgumentException("Property $prop requires a value of type ${prop.propertyType.getType()} but was given $value")
        }
    }

    /**
     * Given a `key` and a [String] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param strValue [String] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [String]
     */
    operator fun set(key: Property, strValue: String) {
        val o = object : PropertyValue(STRING_CLASS) {
            override fun stringValue(): String = strValue
        }
        verifyTypeAndSet(key, STRING_CLASS, strValue ,o)
    }


    /**
     * Given a `key` and a [Long] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param longValue [Long] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [Long]
     */
    operator fun set(key: Property, longValue: Long) {
        val o = object : PropertyValue(LONG_CLASS) {
            override fun longValue(): Long = longValue
        }
        verifyTypeAndSet(key, LONG_CLASS, longValue, o)
    }


    /**
     * Given a `key` and a [Int] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param intValue [Int] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [Int]
     */
    operator fun set(key: Property, intValue: Int) {
        val o = object : PropertyValue(INTEGER_CLASS) {
            override fun integerValue(): Int = intValue
        }
        verifyTypeAndSet(key, INTEGER_CLASS, intValue, o)
    }


    /**
     * Given a `key` and a [IonValue] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param ionValue [IonValue] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [IonValue]
     */
    operator fun set(key: Property, ionValue: IonValue) {
        val o = object : PropertyValue(ION_VALUE_CLASS) {
            override fun ionValue(): IonValue = ionValue
        }
        verifyTypeAndSet(key, ION_VALUE_CLASS, ionValue, o)
    }


    /**
     * Given a `key` and a [TokenType] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param tokenTypeValue [TokenType] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [TokenType]
     */
    operator fun set(key: Property, tokenTypeValue: TokenType) {
        val o = object : PropertyValue(TOKEN_CLASS) {
            override fun tokenTypeValue(): TokenType = tokenTypeValue
        }
        verifyTypeAndSet(key, TOKEN_CLASS, tokenTypeValue, o)
    }


    /**
     * Predicate to check if [property] is already in this [PropertyValueMap]
     *
     * @param property to check for membership in this [PropertyValueMap]
     * @return `true` if `this` [PropertyValueMap] contains [property] as a key, `false` otherwise
     */
    fun hasProperty(property: Property) = map.containsKey(property)


    fun getProperties() = this.map.keys

}

