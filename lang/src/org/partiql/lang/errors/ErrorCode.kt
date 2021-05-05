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

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.syntax.DATE_PART_KEYWORDS
import org.partiql.lang.syntax.TokenType


/** Property Set constants used in [ErrorCode] */
private val LOCATION = setOf(Property.LINE_NUMBER, Property.COLUMN_NUMBER)
private val TOKEN_INFO = setOf(Property.TOKEN_TYPE, Property.TOKEN_VALUE)
private val LOC_TOKEN = LOCATION + (TOKEN_INFO)
private val LOC_TOKEN_STR = LOCATION + (setOf(Property.TOKEN_STRING))


/** Helper function to reduce syntactical overhead of accessing property values as strings. */
private fun PropertyValueMap.getAsString(key: Property, defaultValue: String) =
        this[key]?.toString() ?: defaultValue

/** Each [ErrorCode] contains an immutable set of [Property].
 *  These are the properties used as keys in [PropertyValueMap] created at each error location.
 */
enum class ErrorCode(private val category: ErrorCategory,
                     private val properties: Set<Property>,
                     private val messagePrefix: String) {


    LEXER_INVALID_CHAR(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid character at") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    LEXER_INVALID_OPERATOR(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid operator at") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    LEXER_INVALID_LITERAL(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid literal at") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    LEXER_INVALID_ION_LITERAL(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid ion literal at") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    PARSE_MALFORMED_PARSE_TREE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Internal error - malformed parse tree detected"),

    PARSE_EXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.KEYWORD),
        "expected keyword") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getKeyword(errorContext)
    },

    PARSE_EXPECTED_TOKEN_TYPE(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.EXPECTED_TOKEN_TYPE),
        "expected token of type") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            errorContext?.get(Property.EXPECTED_TOKEN_TYPE)?.tokenTypeValue()?.toString() ?: UNKNOWN +
                "found ${getTokenType(errorContext)}"
    },

    PARSE_EXPECTED_2_TOKEN_TYPES(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.EXPECTED_TOKEN_TYPE_1_OF_2, Property.EXPECTED_TOKEN_TYPE_2_OF_2),
        "unexpected token") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
                "expected ${errorContext?.getAsString(Property.EXPECTED_TOKEN_TYPE_1_OF_2, UNKNOWN)}" +
                " or ${errorContext?.getAsString(Property.EXPECTED_TOKEN_TYPE_2_OF_2, UNKNOWN)}" +
                " but found ${getTokenType(errorContext)}"
    },

    PARSE_EXPECTED_NUMBER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Expected number, found") {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenValue(errorContext)
    },

    PARSE_EXPECTED_TYPE_NAME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected type name, found"),

    PARSE_EXPECTED_WHEN_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected WHEN clause in CASE"),

    PARSE_EXPECTED_WHERE_CLAUSE(
            ErrorCategory.PARSER,
            LOC_TOKEN,
            "expected WHERE clause"),

    PARSE_EXPECTED_CONFLICT_ACTION(
            ErrorCategory.PARSER,
            LOC_TOKEN,
            "expected <conflict action>"),

    PARSE_EXPECTED_RETURNING_CLAUSE(
            ErrorCategory.PARSER,
            LOC_TOKEN,
            "expected <returning mapping>"),

    PARSE_UNSUPPORTED_RETURNING_CLAUSE_SYNTAX(
            ErrorCategory.PARSER,
            LOC_TOKEN,
            "unsupported syntax in RETURNING clause"),

    PARSE_UNSUPPORTED_TOKEN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Unexpected token"),

    PARSE_UNSUPPORTED_LITERALS_GROUPBY(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported literal in GROUP BY"),

    PARSE_EXPECTED_MEMBER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected MEMBER node"),

    PARSE_EXPECTED_DATE_PART(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected one of: [${DATE_PART_KEYWORDS.joinToString()}]"),

    PARSE_UNSUPPORTED_SELECT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported use of SELECT"),

    PARSE_UNSUPPORTED_CASE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported use of CASE"),

    PARSE_UNSUPPORTED_CASE_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Unsupported use of CASE statement"),
    PARSE_UNSUPPORTED_ALIAS(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported syntax for alias, `at` and `as` are supported"),

    PARSE_UNSUPPORTED_SYNTAX(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported Syntax"),

    PARSE_UNKNOWN_OPERATOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported operator"),

    PARSE_INVALID_PATH_COMPONENT(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.TOKEN_TYPE, Property.TOKEN_VALUE),
        "invalid Path component") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return "Invalid path component, expecting either an ${TokenType.IDENTIFIER} or ${TokenType.STAR}, " +
                   "got: ${errorContext?.get(Property.TOKEN_TYPE) ?: UNKNOWN} " +
                   "with value: ${errorContext?.get(Property.TOKEN_VALUE) ?: UNKNOWN}"

        }
    },

    PARSE_MISSING_IDENT_AFTER_AT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "identifier expected after `@` symbol"),

    PARSE_UNEXPECTED_OPERATOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected operator"),

    PARSE_UNEXPECTED_TERM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected term found"),

    PARSE_UNEXPECTED_TOKEN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected token found"),

    PARSE_UNEXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected keyword found"),

    PARSE_EXPECTED_EXPRESSION(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected expression"),

    PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis after CAST"),

    PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis"),

    PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis"),

    PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected right parenthesis"),

    PARSE_EXPECTED_ARGUMENT_DELIMITER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected argument delimiter"),

    PARSE_CAST_ARITY(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.CAST_TO, Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Cast to type ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN} has incorrect arity." +
                "Correct arity is ${errorContext?.get(Property.EXPECTED_ARITY_MIN)?.integerValue() ?: UNKNOWN}.." +
                "${errorContext?.get(Property.EXPECTED_ARITY_MAX)?.integerValue() ?: UNKNOWN}"

    },

    PARSE_INVALID_TYPE_PARAM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid value used for type parameter"),

    PARSE_INVALID_PRECISION_FOR_TIME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid precision used for TIME type"),

    PARSE_INVALID_DATE_STRING(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected date string to be of the format YYYY-MM-DD"),

    PARSE_INVALID_TIME_STRING(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected time string to be of the format HH:MM:SS[.dddd...][+|-HH:MM]"),

    PARSE_EMPTY_SELECT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "found empty SELECT list"),

    PARSE_SELECT_MISSING_FROM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "missing FROM after SELECT list"),

    PARSE_MISSING_OPERATION(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected DML or SELECT operation after FROM"),

    PARSE_MISSING_SET_ASSIGNMENT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected assignment for SET"),

    PARSE_EXPECTED_IDENT_FOR_GROUP_NAME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for GROUP name"),

    PARSE_EXPECTED_IDENT_FOR_ALIAS(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for alias"),

    PARSE_EXPECTED_AS_FOR_LET(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected AS for LET clause"),

    PARSE_UNSUPPORTED_CALL_WITH_STAR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "function call, other than COUNT, with (*) as parameter is not supported"),

    PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Aggregate function calls take 1 argument only"),

    PARSE_NO_STORED_PROCEDURE_PROVIDED(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "No stored procedure provided"),

    PARSE_MALFORMED_JOIN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "malformed use of FROM with JOIN"),

    PARSE_EXPECTED_IDENT_FOR_AT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for AT name"),

    PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Invalid use of * in select list"),

    //SQB = SQuare Bracket
    PARSE_CANNOT_MIX_SQB_AND_WILDCARD_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Cannot mix [] and * in the same expression in a select list"),

    PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOCATION,
        "Other expressions may not be present in the select list when '*' is used without dot notation."),

    //Evaluator errors
    // TODO:  replace uses of this with UNIMPLEMENTED_FEATURE
    EVALUATOR_FEATURE_NOT_SUPPORTED_YET(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.FEATURE_NAME),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Feature '${errorContext?.get(Property.FEATURE_NAME)?.stringValue() ?: UNKNOWN}' not supported yet"
        },

    EVALUATOR_COUNT_DISTINCT_STAR(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "COUNT(DISTINCT *) is not supported"
    },

    EVALUATOR_BINDING_DOES_NOT_EXIST(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME),
        "Binding does not exist") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Binding '${errorContext?.get(Property.BINDING_NAME)?.stringValue() ?: UNKNOWN}' does not exist"
    },

    EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Variable '${errorContext?.get(Property.BINDING_NAME)?.stringValue() ?: UNKNOWN}' " +
            "must appear in the GROUP BY clause or be used in an aggregation function"
    },

    EVALUATOR_UNBOUND_PARAMETER(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_PARAMETER_ORDINAL, Property.BOUND_PARAMETER_COUNT),
        "No parameter bound for position!"),

    EVALUATOR_INVALID_CAST(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.CAST_TO, Property.CAST_FROM),
        ""){
            override fun getErrorMessage(errorContext: PropertyValueMap?): String =
                "Cannot convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
        },

    EVALUATOR_INVALID_CAST_NO_LOCATION(
        ErrorCategory.EVALUATOR,
        setOf(Property.CAST_TO, Property.CAST_FROM),
        ""){
            override fun getErrorMessage(errorContext: PropertyValueMap?): String =
                "Cannot convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
        },

    EVALUATOR_CAST_FAILED(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.CAST_TO, Property.CAST_FROM),
        ""){
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Failed to convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
            "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
        },

    EVALUATOR_CAST_FAILED_NO_LOCATION(
        ErrorCategory.EVALUATOR,
        setOf(Property.CAST_TO, Property.CAST_FROM),
        ""){
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Failed to convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
            "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
        },

    EVALUATOR_NO_SUCH_FUNCTION(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.FUNCTION_NAME),
        ""){
            override fun getErrorMessage(errorContext: PropertyValueMap?): String =
                "No such function: ${errorContext?.get(Property.FUNCTION_NAME)?.stringValue() ?: UNKNOWN} "
        },

    EVALUATOR_NO_SUCH_PROCEDURE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.PROCEDURE_NAME),
        ""){
            override fun getErrorMessage(errorContext: PropertyValueMap?): String =
                "No such stored procedure: ${errorContext?.get(Property.PROCEDURE_NAME)?.stringValue() ?: UNKNOWN} "
        },

    EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX),
        "Incorrect number of arguments to function call"),

    EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX),
        "Incorrect number of arguments to procedure call"),

    EVALUATOR_DATE_FIELD_OUT_OF_RANGE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Date field out of range."),

    EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARGUMENT_TYPES, Property.ACTUAL_ARGUMENT_TYPES, Property.FUNCTION_NAME),
        "Incorrect type of arguments to function call") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid argument types for ${errorContext?.get(Property.FUNCTION_NAME) ?: UNKNOWN}, " +
            "expected: ${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN} " +
            "got: ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES) ?: UNKNOWN}"
    },

    EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARGUMENT_TYPES, Property.ACTUAL_ARGUMENT_TYPES, Property.FUNCTION_NAME),
        "Incorrect type of arguments to procedure call") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid argument types for ${errorContext?.get(Property.FUNCTION_NAME) ?: UNKNOWN}, " +
            "expected: ${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN} " +
            "got: ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES) ?: UNKNOWN}"
    },

    EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_ARGUMENT_TYPES),
        "Incorrect type of arguments for operator '||'") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Incorrect type of arguments for operator '||', " +
            "expected one of ${ExprValueType.values().filter { it.isText }} " +
            "got ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES)}"
    },

    EVALUATOR_INVALID_PRECISION_FOR_TIME(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "invalid precision used for TIME type"),

    /**
     * This is a generic error wrapper for the DateTimeException thrown by Java's [java.time] when attempting to create
     * an instance of [java.time.LocalTime] or [java.time.OffsetTime] when the time field is out of range.
     * The exception is caught by [org.partiql.lang.eval.time.Time.Companion.of].
     */
    EVALUATOR_TIME_FIELD_OUT_OF_RANGE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Invalid value for TIME type"),

    /**
     * This is a generic error thrown whenever Java's [DateTimeFormatter] throws an exception when attempting to
     * parse a timestamp.  Ideally, this doesn't happen and the invalidity is detected by
     * [org.partiql.lang.eval.builtins.timestamp.FormatPattern] instead.  This needs to stick around until we
     * replace [DateTimeFormatter].
     * TODO:  remove this after replacing [DateTimeFormatter].
     */
    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid timestamp format pattern: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains invalid token: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains invalid symbol: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_UNTERMINATED_TIMESTAMP_FORMAT_PATTERN_TOKEN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains unterminated token: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },


    EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN, Property.TIMESTAMP_FORMAT_PATTERN_FIELDS),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' " +
            "requires additional fields '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN_FIELDS)}'."
    },

    EVALUATOR_TIMESTAMP_FORMAT_PATTERN_DUPLICATE_FIELDS(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN, Property.TIMESTAMP_FORMAT_PATTERN_FIELDS),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains multiple format " +
            "specifiers representing the timestamp field '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN_FIELDS)}'."
    },

    EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains a 12-hour hour of " +
            "day format symbol but doesn't also contain an AM/PM field, or it contains a 24-hour hour of day format " +
            "specifier and contains an AM/PM field."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL_FOR_PARSING(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "") {
            override fun getErrorMessage(errorContext: PropertyValueMap?): String =
                "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains a valid format " +
                "symbol that cannot be applied to timestamp parsing."
    },

    EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Failed to parse Ion timestamp"),

    EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE(
        ErrorCategory.EVALUATOR,
        LOCATION+ setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "Failed to parse custom timestamp using the specified format pattern"),

    EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "loss of precision when parsing timestamp"),

    EVALUATOR_INTEGER_OVERFLOW(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Int overflow or underflow"),

    EVALUATOR_AMBIGUOUS_BINDING(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME, Property.BINDING_NAME_MATCHES),
        "Binding name was ambiguous") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Binding name was '${errorContext?.get(Property.BINDING_NAME)}'"
    },

    EVALUATOR_LIKE_INVALID_INPUTS(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.LIKE_VALUE, Property.LIKE_PATTERN, Property.LIKE_ESCAPE),
        "Invalid argument given to LIKE expression") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Given: " +
            "value = ${errorContext?.get(Property.LIKE_VALUE)?.stringValue() ?: UNKNOWN}, " +
            "pattern =  ${errorContext?.get(Property.LIKE_PATTERN)?.stringValue() ?: UNKNOWN}, " +
            "escape char = ${errorContext?.get(Property.LIKE_ESCAPE)?.stringValue() ?: "none given"}"
    },

    EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.LIKE_PATTERN, Property.LIKE_ESCAPE),
        "Pattern contains an invalid or malformed escape sequence"){
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Given: " +
            "pattern =  ${errorContext?.get(Property.LIKE_PATTERN)?.stringValue() ?: UNKNOWN}, " +
            "escape char = ${errorContext?.get(Property.LIKE_ESCAPE)?.stringValue() ?: "none given"}"
    },

    EVALUATOR_NON_INT_LIMIT_VALUE (
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_TYPE),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "LIMIT value must be an integer but found ${errorContext.getProperty(Property.ACTUAL_TYPE)}}"
    },

    EVALUATOR_NEGATIVE_LIMIT(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "LIMIT must not be negative"),

    EVALUATOR_DIVIDE_BY_ZERO(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "/ by zero"),

    EVALUATOR_MODULO_BY_ZERO(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "% by zero"),

    SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
         "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Global variable access is illegal in this context, variable name: '${errorContext.getProperty(Property.BINDING_NAME)}'"
    },

    SEMANTIC_UNBOUND_BINDING(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
         "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such variable named '${errorContext.getProperty(Property.BINDING_NAME)}'"
    },

    SEMANTIC_AMBIGUOUS_BINDING(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "A variable named '${errorContext.getProperty(Property.BINDING_NAME)}' was already defined in this scope"
    },

    /**
     * Indicates incorrectness surrounding arity of [NAry] and [DataType] nodes.
     */
    SEMANTIC_INCORRECT_NODE_ARITY(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.EXPECTED_ARITY_MAX, Property.EXPECTED_ARITY_MIN, Property.ACTUAL_ARITY, Property.FUNCTION_NAME),
        "Incorrect number of arguments for node") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Incorrect number of arguments supplied to `${errorContext.getProperty(Property.FUNCTION_NAME)}`. " +
            "Min = ${errorContext.getProperty(Property.EXPECTED_ARITY_MIN)}, max = ${errorContext.getProperty(Property.EXPECTED_ARITY_MAX)} " +
            "Actual = ${errorContext.getProperty(Property.ACTUAL_ARITY)}"
    },

    SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "HAVING cannot be used without GROUP BY or GROUP ALL"),

    SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "`*` may not be used with other items in a select list"),

    // Generic errors
    UNIMPLEMENTED_FEATURE(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.FEATURE_NAME),
        "") {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Feature '${errorContext?.get(Property.FEATURE_NAME)?.stringValue() ?: UNKNOWN}' not implemented yet"
    };

    protected fun getTokenString(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_STRING)?.stringValue() ?: UNKNOWN

    protected fun getTokenValue(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_VALUE)?.ionValue()?.toString() ?: UNKNOWN

    protected fun getTokenType(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_TYPE)?.tokenTypeValue()?.toString() ?: UNKNOWN

    protected fun getKeyword(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.KEYWORD)?.stringValue() ?: UNKNOWN

    protected fun getTokenTypeAndTokenValue(errorContext: PropertyValueMap?): String =
        getTokenType(errorContext) + " : " + getTokenValue(errorContext)


    open protected fun detailMessagePrefix(): String = messagePrefix

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

    fun errorCategory(): String = category.toString()


    fun getProperties(): Set<Property> = properties

}

private fun PropertyValueMap?.getProperty(prop: Property): String =
    this?.get(prop)?.toString() ?: UNKNOWN
