/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.errorhandling

import com.amazon.ion.IonValue
import com.amazon.ionsql.syntax.TokenType
import java.util.*


private const val  SEPARATOR = ", "
const val UNKNOWN: String = "<UNKNOWN>"

/**
 * Categories for errors
 */
enum class ErrorCategory {
    LEXER,
    PARSER,
    EVALUATOR
}

/** Each [ErrorCode] contains an immutable set of [Property]; these are the properties
 * used as keys in [PropertyBag] created at each error location.
 */
enum class ErrorCode constructor(val category: ErrorCategory, vararg props: Property) {

    LEXER_INVALID_CHAR(
        ErrorCategory.LEXER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_STRING) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getTokenString(errorContext)

        override fun detailMessagePrefix(): String  = "invalid character at"
    },
    LEXER_INVALID_OPERATOR(
        ErrorCategory.LEXER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_STRING) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getTokenString(errorContext)
        override fun detailMessagePrefix(): String  = "invalid operator at"

    },
    LEXER_INVALID_LITERAL(
        ErrorCategory.LEXER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_STRING) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getTokenString(errorContext)
        override fun detailMessagePrefix(): String  = "invalid literal at"

    },
    PARSE_EXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE,
        Property.KEYWORD) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getKeyword(errorContext)
        override fun detailMessagePrefix(): String  = "expected keyword"

    },
    PARSE_EXPECTED_TOKEN_TYPE(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getTokenType(errorContext)
        override fun detailMessagePrefix(): String  = "expected token of type"

    },
    PARSE_EXPECTED_NUMBER(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE) {
        override fun detailMessageSuffix(errorContext: PropertyBag?): String =
            getTokenValue(errorContext)
        override fun detailMessagePrefix(): String  = "Expected number, found"

    },
    PARSE_EXPECTED_TYPE_NAME(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected type name, found"

    },
    PARSE_EXPECTED_WHEN_CLAUSE(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected WHEN clause in CASE"

    },
    PARSE_UNSUPPORTED_TOKEN(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unexpected token"

    },
    PARSE_UNSUPPORTED_LITERALS_GROUPBY(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported literal in GROUP BY"

    },
    PARSE_EXPECTED_MEMBER(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected MEMBER node"

    },
    PARSE_UNSUPPORTED_SELECT(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported use of SELECT"

    },
    PARSE_UNSUPPORTED_CASE(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported use of CASE"

    },
    PARSE_UNSUPPORTED_CASE_CLAUSE(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported use of CASE statement"

    },
    PARSE_UNSUPPORTED_ALIAS(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported syntax for alias, `at` and `as` are supported"

    },
    PARSE_UNSUPPORTED_SYNTAX(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported Syntax"

    },
    PARSE_UNKNOWN_OPERATOR(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unsupported operator"

    },
    PARSE_INVALID_PATH_COMPONENT(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Invalid Path component"

    },
    PARSE_MISSING_IDENT_AFTER_AT(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Identifier expected after `@` symbol"

    },
    PARSE_UNEXPECTED_OPERATOR(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unexpected operator"

    },
    PARSE_UNEXPECTED_TERM(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unexpected term found"

    },
    PARSE_UNEXPECTED_TOKEN(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unexpected token found"

    },
    PARSE_UNEXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Unexpected keyword found"

    },
    PARSE_EXPECTED_EXPRESSION(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected expression"

    },
    PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected left parenthesis after CAST"

    },
    PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected left parenthesis"

    },
    PARSE_CAST_ARITY(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE,
        Property.CAST_TO,
        Property.EXPECTED_ARITY_MIN,
        Property.EXPECTED_ARITY_MAX){
        override fun detailMessagePrefix(): String  = ""
        override fun getErrorMessage(errorContext: PropertyBag?): String =
            "Cast to type ${errorContext?.getProperty(Property.CAST_TO, String::class.javaObjectType) ?: UNKNOWN} has incorrect arity." +
                "Correct arity is ${errorContext?.getProperty(Property.EXPECTED_ARITY_MIN, Int::class.javaObjectType) ?: UNKNOWN}.." +
                "${errorContext?.getProperty(Property.EXPECTED_ARITY_MAX, Int::class.javaObjectType) ?: UNKNOWN}"

    },
    PARSE_INVALID_TYPE_PARAM(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Invalid value used for type parameter"

    },
    PARSE_EMPTY_SELECT(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Found empty SELECT list"

    },
    PARSE_SELECT_MISSING_FROM(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Missing FROM after SELECT list"

    },
    PARSE_EXPECTED_IDENT_FOR_GROUP_NAME(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected identifier for GROUP name"

    },
    PARSE_EXPECTED_IDENT_FOR_ALIAS(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected identifier for alias"

    },
    PARSE_EXPECTED_IDENT_FOR_AT(
        ErrorCategory.PARSER,
        Property.LINE_NO,
        Property.COLUMN_NO,
        Property.TOKEN_TYPE,
        Property.TOKEN_VALUE){
        override fun detailMessagePrefix(): String  = "Expected identifier for AT name"

    },
    ;


    protected fun getTokenString(errorContext: PropertyBag?): String =
        errorContext?.getProperty(Property.TOKEN_STRING, String::class.javaObjectType) ?: UNKNOWN

    protected fun getTokenValue(errorContext: PropertyBag?): String =
        errorContext?.getProperty(Property.TOKEN_VALUE, IonValue::class.javaObjectType)?.toString() ?: UNKNOWN

    protected fun getTokenType(errorContext: PropertyBag?): String =
        errorContext?.getProperty(Property.TOKEN_TYPE, TokenType::class.javaObjectType)?.toString() ?: UNKNOWN

    protected fun getKeyword(errorContext: PropertyBag?): String =
        errorContext?.getProperty(Property.KEYWORD, String::class.javaObjectType) ?: UNKNOWN

    protected fun getTokenTypeAndTokenValue(errorContext: PropertyBag?): String =
        getTokenType(errorContext) + " : " + getTokenValue(errorContext)


    private val properties: MutableSet<Property>


    abstract protected fun detailMessagePrefix(): String

    open protected fun detailMessageSuffix(errorContext: PropertyBag?): String =
        getTokenTypeAndTokenValue(errorContext)

    /**
     * Given an [errorContext] generate a detailed error message.
     *
     * Template method.
     *
     * @param errorContext  that contains information about the error
     * @return detailed error message as a [String]
     */
    open fun getErrorMessage(errorContext: PropertyBag?): String =
        detailMessagePrefix() + SEPARATOR + detailMessageSuffix(errorContext)

    fun errorCategory() : String =
      when (this.category) {
          ErrorCategory.LEXER -> "Lexer error"
          ErrorCategory.PARSER -> "Parser error"
          ErrorCategory.EVALUATOR -> "Evaluator error"
      }

    fun getProperties(): Set<Property> {
        return Collections.unmodifiableSet(properties)
    }

    init {
        this.properties = HashSet()
        this.properties.addAll(listOf(*props))
    }
}


/** Each possible value that can be reported as part of an error maps to a
 * [Property].
 *
 * Each property contains a string name and a [type] of the values that this property can have.
 *
 * @param propertyName string name (internal use)
 * @param type [Class] of object's that this property can hold in a [PropertyBag]
 *
 */
enum class Property(val propertyName: String, val type: Class<*>) {

    LINE_NO("line_no", Long::class.javaObjectType),
    COLUMN_NO("column_no", Long::class.javaObjectType),
    TOKEN_STRING("token_string", String::class.javaObjectType),
    CAST_TO("cast_to", String::class.javaObjectType),
    KEYWORD("keyword", String::class.javaObjectType),
    TOKEN_TYPE("token_type", TokenType::class.javaObjectType),
    TOKEN_VALUE("token_value", IonValue::class.javaObjectType),
    EXPECTED_ARITY_MIN("arity_min", Int::class.javaObjectType),
    EXPECTED_ARITY_MAX("arity_max", Int::class.javaObjectType)
}


/**
 * A typed bag (map) of properties used to capture an error context.
 */
class PropertyBag(private val bag: MutableMap<Property, Any?> = HashMap()) {


    /**
     * Given a [Property] and a [Class] retrieve the value mapped to [p] in this bag.
     * If the [type] provided does not match the [p]'s type we throw an [IllegalArgumentException]
     *
     * @param p key to be retrieved from the bag
     * @param type type for the value being retrieved. This [type] **must** much the [Property]'s ([p]'s) type
     * @return the value stored in this [PropertyBag]
     *
     * @throws IllegalArgumentException when the [type] passed as argument does not match the [Property]'s type
     */
    fun <T> getProperty(p: Property, type: Class<T>): T {
        if (p.type == type) {
            return bag[p] as T
        } else {
            throw IllegalArgumentException(
                "Property's type [${p.type}] does not match type (second argument) provided [$type]")
        }
    }

    /**
     * Given a [property] and a [value] add/update the property bag and return the updated instance of [PropertyBag],
     * i.e., `this`
     *
     * @param property [Property] to be used as the key when inserting/updating this [PropertyBag]
     * @param value any reference type (can be `null`) to be used as the value for [property] in this [PropertyBag]
     * @return updated instance (`this`) of the [PropertyBag]
     *
     * @throws IllegalArgumentException when the [value] provided is type-able (assignable) as `property.type`
     */
    fun addProperty(property: Property, value: Any?): PropertyBag =
        when (value) {
            null -> {
                bag.put(property, value)
                this
            }
            else -> {
                if (property.type.isAssignableFrom(value::class.javaObjectType)) {
                    bag.put(property, value)
                    this
                } else {
                    throw IllegalArgumentException(
                        "Type mismatch: value [$value] with runtime type [ ${value::class.java} ] " +
                            "must match it's property's type ${property.type}")
                }
            }
        }

    /**
     * Given a [Property], [p] **already in this [PropertyBag], update it's mapping to [value].
     *
     * @param p [Property] to be updated
     * @param value new value to be set for [p] in this [PropertyBag]
     * @return updated instance (`this`) of the [PropertyBag]
     *
     * @throws IllegalArgumentException when [value]  provided is type-able (assignable) as `property.type`
     *
     */
    fun addPropertyIfKeyNotPresent(p: Property, value: Any?) : PropertyBag {
        if (bag.containsKey(p)) {
            return this
        } else {
            return addProperty(p, value)
        }
    }

    /**
     * Predicate to check if [property] is already in this [PropertyBag]
     *
     * @param property to check for membership in this [PropertyBag]
     * @return `true` if `this` [PropertyBag] contains [property] as a key, `false` otherwise
     */
    fun hasProperty(property: Property): Boolean {
        return bag.containsKey(property)
    }

    fun getProperties() = this.bag.keys

}

