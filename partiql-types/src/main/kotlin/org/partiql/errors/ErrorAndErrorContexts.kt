/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.errors

import com.amazon.ion.IonValue
import java.util.EnumMap

public const val UNKNOWN: String = "<UNKNOWN>"

/**
 * Categories for errors. Should map to stages in the Compiler and Evaluator.
 */
public enum class ErrorCategory(public val message: String) {
    LEXER("Lexer Error"),
    PARSER("Parser Error"),
    SEMANTIC("Semantic Error"),
    EVALUATOR("Evaluator Error");
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
public enum class Property(public val propertyName: String, public val propertyType: PropertyType) {
    LINE_NUMBER("line_no", PropertyType.LONG_CLASS),
    COLUMN_NUMBER("column_no", PropertyType.LONG_CLASS),
    TOKEN_STRING("token_string", PropertyType.STRING_CLASS),
    CAST_TO("cast_to", PropertyType.STRING_CLASS),
    CAST_FROM("cast_from", PropertyType.STRING_CLASS),
    TOKEN_DESCRIPTION("token_description", PropertyType.STRING_CLASS),
    TOKEN_VALUE("token_value", PropertyType.ION_VALUE_CLASS),
    EXPECTED_ARITY_MIN("arity_min", PropertyType.INTEGER_CLASS),
    EXPECTED_ARITY_MAX("arity_max", PropertyType.INTEGER_CLASS),
    ACTUAL_ARITY("actual_arity", PropertyType.INTEGER_CLASS),
    EXPECTED_PARAMETER_ORDINAL("expected_parameter_ordinal", PropertyType.INTEGER_CLASS),
    BOUND_PARAMETER_COUNT("bound_parameter_count", PropertyType.INTEGER_CLASS),
    TIMESTAMP_FORMAT_PATTERN("timestamp_format_pattern", PropertyType.STRING_CLASS),
    TIMESTAMP_FORMAT_PATTERN_FIELDS("timestamp_format_pattern_fields", PropertyType.STRING_CLASS),
    BINDING_NAME("binding_name", PropertyType.STRING_CLASS),
    BINDING_NAME_MATCHES("binding_name_matches", PropertyType.STRING_CLASS),
    LIKE_VALUE("value_to_match", PropertyType.STRING_CLASS),
    LIKE_PATTERN("pattern", PropertyType.STRING_CLASS),
    LIKE_ESCAPE("escape_char", PropertyType.STRING_CLASS),
    FUNCTION_NAME("function_name", PropertyType.STRING_CLASS),
    ARGUMENT_POSITION("argument_position", PropertyType.INTEGER_CLASS),
    PROCEDURE_NAME("procedure_name", PropertyType.STRING_CLASS),
    EXPECTED_ARGUMENT_TYPES("expected_types", PropertyType.STRING_CLASS),
    ACTUAL_ARGUMENT_TYPES("actual_types", PropertyType.STRING_CLASS),
    FEATURE_NAME("FEATURE_NAME", PropertyType.STRING_CLASS),
    ACTUAL_TYPE("ACTUAL_TYPE", PropertyType.STRING_CLASS),
    INT_SIZE_IN_BYTES("INT_SIZE", PropertyType.INTEGER_CLASS),
    EXPECTED_STATIC_TYPE("EXPECTED_STATIC_TYPE", PropertyType.STRING_CLASS),
    CONSTANT("CONSTANT", PropertyType.STRING_CLASS),
    MESSAGE("MESSAGE", PropertyType.STRING_CLASS)
}

/**
 * A [PropertyValue] is the top level type for all values that appear as properties in error codes.
 * For each type of value that can be a [Property] there is a method to allow clients to obtain the
 * correctly typed value.
 */
public abstract class PropertyValue(public val type: PropertyType) {
    public open fun stringValue(): String = throw IllegalArgumentException("Property value is of type $type and not String")
    public open fun longValue(): Long = throw IllegalArgumentException("Property value is of type $type and not Long")
    public open fun tokenTypeValue(): String = throw IllegalArgumentException("Property value is of type $type and not String")
    public open fun integerValue(): Int = throw IllegalArgumentException("Property value is of type $type and not Integer")
    public open fun ionValue(): IonValue = throw IllegalArgumentException("Property value is of type $type and not IonValue")

    public val value: Any
        get() = when (type) {
            PropertyType.LONG_CLASS -> longValue()
            PropertyType.STRING_CLASS -> stringValue()
            PropertyType.INTEGER_CLASS -> integerValue()
            PropertyType.TOKEN_CLASS -> tokenTypeValue()
            PropertyType.ION_VALUE_CLASS -> ionValue()
        }

    override fun toString(): String =
        when (type) {
            PropertyType.ION_VALUE_CLASS -> (value as IonValue).toPrettyString()
            else -> value.toString()
        }

    /** For debugging purposes (unit tests) only.*/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PropertyValue) return false
        if (type != other.type) return false
        if (value != other.value) return false

        return true
    }

    /** For debugging purposes (unit tests) only.*/
    override fun hashCode(): Int = this.value.hashCode()
}

/**
 * A [PropertyType] is a top level type for all types of values that appear as properties in error codes.
 * Clients can access the type (as a `Class<*>`) of a property's value through [getType()].
 *
 */
public enum class PropertyType(private val type: Class<*>) {
    LONG_CLASS(Long::class.javaObjectType),
    STRING_CLASS(String::class.javaObjectType),
    INTEGER_CLASS(Int::class.javaObjectType),
    TOKEN_CLASS(String::class.javaObjectType),
    ION_VALUE_CLASS(IonValue::class.javaObjectType);

    public fun getType(): Class<*> = type
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
public class PropertyValueMap(private val map: EnumMap<Property, PropertyValue> = EnumMap(Property::class.java)) {

    /**
     * Given a [Property] retrieve the value mapped to [key] in this map.
     *
     *
     * @param key to be retrieved from the map
     * @return the value stored in this [PropertyValueMap] as a [PropertyValue], `null` if key is not present
     *
     */
    public operator fun get(key: Property): PropertyValue? = map[key]

    private fun <T> verifyTypeAndSet(prop: Property, expectedType: PropertyType, value: T, pValue: PropertyValue) {
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
    public operator fun set(key: Property, strValue: String) {
        val o = object : PropertyValue(PropertyType.STRING_CLASS) {
            override fun stringValue(): String = strValue
        }
        verifyTypeAndSet(key, PropertyType.STRING_CLASS, strValue, o)
    }

    /**
     * Given a `key` and a [Long] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param longValue [Long] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [Long]
     */
    public operator fun set(key: Property, longValue: Long) {
        val o = object : PropertyValue(PropertyType.LONG_CLASS) {
            override fun longValue(): Long = longValue
        }
        verifyTypeAndSet(key, PropertyType.LONG_CLASS, longValue, o)
    }

    /**
     * Given a `key` and a [Int] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param intValue [Int] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [Int]
     */
    public operator fun set(key: Property, intValue: Int) {
        val o = object : PropertyValue(PropertyType.INTEGER_CLASS) {
            override fun integerValue(): Int = intValue
        }
        verifyTypeAndSet(key, PropertyType.INTEGER_CLASS, intValue, o)
    }

    /**
     * Given a `key` and a [IonValue] value, insert the key-value pair into the [PropertyValueMap].
     *
     * @param key to be added into the [PropertyValueMap]
     * @param ionValue [IonValue] value to be associated with `key` in the [PropertyValueMap]
     *
     * @throws [IllegalArgumentException] if the [Property] used as `key` requires values of type **other than** [IonValue]
     */
    public operator fun set(key: Property, ionValue: IonValue) {
        val o = object : PropertyValue(PropertyType.ION_VALUE_CLASS) {
            override fun ionValue(): IonValue = ionValue
        }
        verifyTypeAndSet(key, PropertyType.ION_VALUE_CLASS, ionValue, o)
    }

    /**
     * Predicate to check if [property] is already in this [PropertyValueMap]
     *
     * @param property to check for membership in this [PropertyValueMap]
     * @return `true` if `this` [PropertyValueMap] contains [property] as a key, `false` otherwise
     */
    public fun hasProperty(property: Property): Boolean = map.containsKey(property)

    @Suppress("UNUSED")
    public fun getProperties(): Set<Property> = this.map.keys

    /** Creates a human-readable representation of this [PropertyValueMap].  For debugging only. */
    override fun toString(): String =
        this.map.entries.sortedBy { it.key }.joinToString(", ", "propertyValueMapOf(", ")") {
            val value = when (it.value.value) {
                is String -> "\"${it.value}\""
                else -> it.value.toString()
            }
            "Property.${it.key} to $value"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PropertyValueMap

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }
}
