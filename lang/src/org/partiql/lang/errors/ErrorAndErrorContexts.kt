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

package org.partiql.lang.errors

import com.amazon.ion.IonValue
import org.partiql.lang.errors.PropertyType.*
import org.partiql.lang.syntax.*
import java.util.*

internal const val UNKNOWN: String = "<UNKNOWN>"


/**
 * Categories for errors. Should map to stages in the Compiler and Evaluator.
 */
enum class ErrorCategory(val message: String) {
    LEXER ("Lexer Error"),
    PARSER ("Parser Error"),
    SEMANTIC ("Semantic Error"),
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
    CAST_FROM("cast_from", STRING_CLASS),
    KEYWORD("keyword", STRING_CLASS),
    TOKEN_TYPE("token_type", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE("expected_token_type", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE_1_OF_2("expected_token_type_1_of_2", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE_2_OF_2("expected_token_type_2_of_2", TOKEN_CLASS),
    TOKEN_VALUE("token_value", ION_VALUE_CLASS),
    EXPECTED_ARITY_MIN("arity_min", INTEGER_CLASS),
    EXPECTED_ARITY_MAX("arity_max", INTEGER_CLASS),
    ACTUAL_ARITY("actual_arity", INTEGER_CLASS),
    EXPECTED_PARAMETER_ORDINAL("expected_parameter_ordinal", INTEGER_CLASS),
    BOUND_PARAMETER_COUNT("bound_parameter_count", INTEGER_CLASS),
    TIMESTAMP_FORMAT_PATTERN("timestamp_format_pattern", STRING_CLASS),
    TIMESTAMP_FORMAT_PATTERN_FIELDS("timestamp_format_pattern_fields", STRING_CLASS),
    TIMESTAMP_STRING("timestamp_string", STRING_CLASS),
    BINDING_NAME("binding_name", STRING_CLASS),
    BINDING_NAME_MATCHES("binding_name_matches", STRING_CLASS),
    LIKE_VALUE("value_to_match", STRING_CLASS),
    LIKE_PATTERN("pattern", STRING_CLASS),
    LIKE_ESCAPE("escape_char", STRING_CLASS),
    FUNCTION_NAME("function_name", STRING_CLASS),
    PROCEDURE_NAME("procedure_name", STRING_CLASS),
    EXPECTED_ARGUMENT_TYPES("expected_types", STRING_CLASS),
    ACTUAL_ARGUMENT_TYPES("actual_types", STRING_CLASS),
    FEATURE_NAME("FEATURE_NAME", STRING_CLASS),
    ACTUAL_TYPE("ACTUAL_TYPE", STRING_CLASS)
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

    val value: Any
        get() = when (type) {
            LONG_CLASS      -> longValue()
            STRING_CLASS    -> stringValue()
            INTEGER_CLASS   -> integerValue()
            TOKEN_CLASS     -> tokenTypeValue()
            ION_VALUE_CLASS -> ionValue()
        }

    override fun toString(): String =
        when (type) {
            ION_VALUE_CLASS -> (value as IonValue).toPrettyString()
            else -> value.toString()
        }
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

    fun getType() = type
}

/**
 * A typed map of properties used to capture an contextual information about an error.
 *
 * At each error location and for the specific error code `ec` the implementation of PartiQL:
 *
 *  1. Creates a new [PropertyValueMap]
 *  1. **Attempts** to add to add values for **all** [Property] found in `ec.getProperties()` set
 *
 *  It may be the case that the implementation was not able to populate one of the [Property] of `ec.getProperties()`.
 *  In that case the [PropertyValueMap] will **not** contain a key-value pair for that [Property].
 *
 *  Absence of a key means that there is no information. Clients **should** test a [Property] for membership in
 *  [PropertyValueMap].
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

