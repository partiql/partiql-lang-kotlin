/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.errors

import com.amazon.ion.IonValue
import com.amazon.ionsql.errors.ErrorCategory.*
import com.amazon.ionsql.errors.Property.*
import com.amazon.ionsql.syntax.TokenType
import java.util.*


internal const val UNKNOWN: String = "<UNKNOWN>"

/**
 * Categories for errors
 */
enum class ErrorCategory {
    LEXER {
        override fun toString(): String {
            return "Lexer Error"
        }
    }
    ,
    PARSER {
        override fun toString(): String {
            return "Parser Error"
        }
    },
    EVALUATOR {
        override fun toString(): String {
            return "Evaluator Error"
        }
    }
}

/** Each [ErrorCode] contains an immutable set of [Property].
 *  These are the properties used as keys in [PropertyBag] created at each error location.
 */

/** Property Set constants used in [ErrorCode] */
private val LOCATION = setOf(LINE_NUMBER, COLUMN_NUMBER)
private val TOKEN_INFO = setOf(TOKEN_TYPE, TOKEN_VALUE)
private val LOC_TOKEN = LOCATION.union(TOKEN_INFO)
private val LOC_TOKEN_STR = LOCATION.union(setOf(TOKEN_STRING))

enum class ErrorCode(private val category: ErrorCategory, private val properties: Set<Property>) {




    LEXER_INVALID_CHAR(
        LEXER,
        LOC_TOKEN_STR) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)

        override fun detailMessagePrefix(): String = "invalid character at"
    },
    LEXER_INVALID_OPERATOR(
        LEXER,
        LOC_TOKEN_STR) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)

        override fun detailMessagePrefix(): String = "invalid operator at"

    },
    LEXER_INVALID_LITERAL(
        LEXER,
        LOC_TOKEN_STR) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)

        override fun detailMessagePrefix(): String = "invalid literal at"

    },
    PARSE_EXPECTED_KEYWORD(
        PARSER,
        LOC_TOKEN.union(setOf(KEYWORD))) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getKeyword(errorContext)

        override fun detailMessagePrefix(): String = "expected keyword"

    },
    PARSE_EXPECTED_TOKEN_TYPE(
        PARSER,
        LOC_TOKEN.union(setOf(EXPECTED_TOKEN_TYPE))) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            errorContext?.get(EXPECTED_TOKEN_TYPE)?.tokenTypeValue()?.toString() ?: UNKNOWN +
                "found ${getTokenType(errorContext)}"

        override fun detailMessagePrefix(): String = "expected token of type"

    },
    PARSE_EXPECTED_NUMBER(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenValue(errorContext)

        override fun detailMessagePrefix(): String = "Expected number, found"

    },
    PARSE_EXPECTED_TYPE_NAME(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected type name, found"

    },
    PARSE_EXPECTED_WHEN_CLAUSE(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected WHEN clause in CASE"

    },
    PARSE_UNSUPPORTED_TOKEN(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unexpected token"

    },
    PARSE_UNSUPPORTED_LITERALS_GROUPBY(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported literal in GROUP BY"

    },
    PARSE_EXPECTED_MEMBER(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected MEMBER node"

    },
    PARSE_UNSUPPORTED_SELECT(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported use of SELECT"

    },
    PARSE_UNSUPPORTED_CASE(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported use of CASE"

    },
    PARSE_UNSUPPORTED_CASE_CLAUSE(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported use of CASE statement"

    },
    PARSE_UNSUPPORTED_ALIAS(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported syntax for alias, `at` and `as` are supported"

    },
    PARSE_UNSUPPORTED_SYNTAX(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported Syntax"

    },
    PARSE_UNKNOWN_OPERATOR(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unsupported operator"

    },
    PARSE_INVALID_PATH_COMPONENT(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Invalid Path component"

    },
    PARSE_MISSING_IDENT_AFTER_AT(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Identifier expected after `@` symbol"

    },
    PARSE_UNEXPECTED_OPERATOR(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unexpected operator"

    },
    PARSE_UNEXPECTED_TERM(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unexpected term found"

    },
    PARSE_UNEXPECTED_TOKEN(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unexpected token found"

    },
    PARSE_UNEXPECTED_KEYWORD(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Unexpected keyword found"

    },
    PARSE_EXPECTED_EXPRESSION(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected expression"

    },
    PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected left parenthesis after CAST"

    },
    PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected left parenthesis"

    },
    PARSE_CAST_ARITY(
        PARSER,
        LOC_TOKEN.union(setOf(CAST_TO, EXPECTED_ARITY_MIN, EXPECTED_ARITY_MAX))) {
        override fun detailMessagePrefix(): String = ""
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Cast to type ${errorContext?.get(CAST_TO)?.stringValue() ?: UNKNOWN} has incorrect arity." +
                "Correct arity is ${errorContext?.get(EXPECTED_ARITY_MIN)?.integerValue() ?: UNKNOWN}.." +
                "${errorContext?.get(EXPECTED_ARITY_MAX)?.integerValue() ?: UNKNOWN}"

    },
    PARSE_INVALID_TYPE_PARAM(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Invalid value used for type parameter"

    },
    PARSE_EMPTY_SELECT(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Found empty SELECT list"

    },
    PARSE_SELECT_MISSING_FROM(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Missing FROM after SELECT list"

    },
    PARSE_EXPECTED_IDENT_FOR_GROUP_NAME(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected identifier for GROUP name"

    },
    PARSE_EXPECTED_IDENT_FOR_ALIAS(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected identifier for alias"

    },
    PARSE_UNSUPPORTED_CALL_WITH_STAR(
        PARSER,
        LOC_TOKEN){
        override fun detailMessagePrefix(): String = "Function call, other than COUNT, with (*) as parameter is not supported"

    },

    PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL(
        PARSER,
        LOC_TOKEN){
        override fun detailMessagePrefix(): String = "Aggregate function calls take 1 argument only"

    },

    PARSE_MALFORMED_JOIN(
        PARSER,
        LOC_TOKEN){
        override fun detailMessagePrefix(): String = "Malformed use of FROM with JOIN"
    },

    PARSE_EXPECTED_IDENT_FOR_AT(
        PARSER,
        LOC_TOKEN) {
        override fun detailMessagePrefix(): String = "Expected identifier for AT name"

    },
    ;


    constructor(category: ErrorCategory, vararg props: Property) : this(category, setOf(*props))

    protected fun getTokenString(errorContext: PropertyValueMap?): String =
        errorContext?.get(TOKEN_STRING)?.stringValue() ?: UNKNOWN

    protected fun getTokenValue(errorContext: PropertyValueMap?): String =
        errorContext?.get(TOKEN_VALUE)?.ionValue()?.toString() ?: UNKNOWN

    protected fun getTokenType(errorContext: PropertyValueMap?): String =
        errorContext?.get(TOKEN_TYPE)?.tokenTypeValue()?.toString() ?: UNKNOWN

    protected fun getKeyword(errorContext: PropertyValueMap?): String =
        errorContext?.get(KEYWORD)?.stringValue() ?: UNKNOWN

    protected fun getTokenTypeAndTokenValue(errorContext: PropertyValueMap?): String =
        getTokenType(errorContext) + " : " + getTokenValue(errorContext)


    abstract protected fun detailMessagePrefix(): String

    open protected fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
        getTokenTypeAndTokenValue(errorContext)

    /**
     * Given an [errorContext] generate a detailed error message.
     *
     * Template method.
     *
     * @param errorContext  that contains information about the error
     * @return detailed error message as a [String]
     */
    open fun getErrorMessage(errorContext: PropertyValueMap?): String =
        "${detailMessagePrefix()}, ${detailMessageSuffix(errorContext)}"

    fun errorCategory(): String = this.category.toString()


    fun getProperties(): Set<Property> {
        return Collections.unmodifiableSet(properties)
    }


}


internal val LONG_CLASS = Long::class.javaObjectType
internal val STRING_CLASS = String::class.javaObjectType
internal val INTEGER_CLASS = Int::class.javaObjectType
internal val TOKEN_CLASS = TokenType::class.javaObjectType
internal val ION_VALUE_CLASS = IonValue::class.javaObjectType

/** Each possible value that can be reported as part of an error maps to a
 * [Property].
 *
 * Each property contains a string name and a [type] of the values that this property can have.
 *
 * @param propertyName string name (internal use)
 * @param type [Class] of object's that this property can hold in a [PropertyValueMap]
 *
 */
enum class Property(val propertyName: String, val type: Class<*>) {


    LINE_NUMBER("line_no", LONG_CLASS),
    COLUMN_NUMBER("column_no", LONG_CLASS),
    TOKEN_STRING("token_string", STRING_CLASS),
    CAST_TO("cast_to", STRING_CLASS),
    KEYWORD("keyword", STRING_CLASS),
    TOKEN_TYPE("token_type", TOKEN_CLASS),
    EXPECTED_TOKEN_TYPE("expected_token_type", TOKEN_CLASS),
    TOKEN_VALUE("token_value", ION_VALUE_CLASS),
    EXPECTED_ARITY_MIN("arity_min", INTEGER_CLASS),
    EXPECTED_ARITY_MAX("arity_max", INTEGER_CLASS)


}

abstract class PropertyValue(val value: Any?, val type: Class<*>) {
    open fun stringValue(): String? = throw IllegalArgumentException("Property value is of type $type and not String")
    open fun longValue(): Long? = throw IllegalArgumentException("Property value is of type $type and not Long")
    open fun tokenTypeValue(): TokenType? = throw IllegalArgumentException("Property value is of type $type and not TokenType")
    open fun integerValue(): Int? = throw IllegalArgumentException("Property value is of type $type and not Integer")
    open fun ionValue(): IonValue? = throw IllegalArgumentException("Property value is of type $type and not IonValue")
}


/**
 * A typed map of properties used to capture an error context.
 */
class PropertyValueMap(private val map: EnumMap<Property, PropertyValue?> = EnumMap(Property::class.java)) {


    /**
     * Given a [Property]  retrieve the value mapped to [p] in this map.
     *
     *
     * @param p key to be retrieved from the map
     * @return the value stored in this [PropertyValueMap] as a [PropertyValue]
     *
     * @throws IllegalArgumentException when the [type] passed as argument does not match the [Property]'s type
     */
    operator fun get(key: Property): PropertyValue? = map[key]


    operator fun set(key: Property, strValue: String?) {
        if (key.type == STRING_CLASS) {
            map[key] = object : PropertyValue(strValue, STRING_CLASS) {
                override fun stringValue(): String? = strValue
            }
        } else {
            throw IllegalArgumentException("Property $key requires a value of type ${key.type} but was given $strValue")
        }
    }


    operator fun set(key: Property, longValue: Long?) {
        if (key.type == LONG_CLASS) {
            map[key] = object : PropertyValue(longValue, LONG_CLASS) {
                override fun longValue(): Long? = longValue
            }
        } else {
            throw IllegalArgumentException("Property $key requires a value of type ${key.type} but was given $longValue")
        }
    }


    operator fun set(key: Property, intValue: Int?) {
        if (key.type == INTEGER_CLASS) {
            map[key] = object : PropertyValue(intValue, INTEGER_CLASS) {
                override fun integerValue(): Int? = intValue
            }
        } else {
            throw IllegalArgumentException("Property $key requires a value of type ${key.type} but was given $intValue")
        }
    }


    operator fun set(key: Property, ionValue: IonValue?) {
        if (key.type == ION_VALUE_CLASS) {
            map[key] = object : PropertyValue(ionValue, ION_VALUE_CLASS) {
                override fun ionValue(): IonValue? = ionValue
            }
        } else {
            throw IllegalArgumentException("Property $key requires a value of type ${key.type} but was given $ionValue")
        }
    }

    operator fun set(key: Property, tokenTypeValue: TokenType?) {
        if (key.type == TOKEN_CLASS) {
            map[key] = object : PropertyValue(tokenTypeValue, TOKEN_CLASS) {
                override fun tokenTypeValue(): TokenType? = tokenTypeValue
            }
        } else {
            throw IllegalArgumentException("Property $key requires a value of type ${key.type} but was given $tokenTypeValue")
        }
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

