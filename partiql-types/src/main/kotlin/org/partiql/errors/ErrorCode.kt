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

/** Property Set constants used in [ErrorCode] */
private val LOCATION = setOf(Property.LINE_NUMBER, Property.COLUMN_NUMBER)
private val TOKEN_INFO = setOf(Property.TOKEN_DESCRIPTION, Property.TOKEN_VALUE)
private val LOC_TOKEN = LOCATION + (TOKEN_INFO)
private val LOC_TOKEN_STR = LOCATION + (setOf(Property.TOKEN_STRING))

public enum class ErrorBehaviorInPermissiveMode {
    THROW_EXCEPTION, RETURN_MISSING
}

public const val UNBOUND_QUOTED_IDENTIFIER_HINT: String =
    "Hint: did you intend to use single quotes (') here instead of double quotes (\")? " +
        "Use single quotes (') for string literals and double quotes (\") for quoted identifiers."

/** Each [ErrorCode] contains an immutable set of [Property].
 *  These are the properties used as keys in [PropertyValueMap] created at each error location.
 *  @property errorBehaviorInPermissiveMode This enum is used during evaluation to determine the behavior of the error.
 *  - If it is THROW_EXCEPTION, which is the default behavior, evaluator will throw an EvaluationException in the permissive mode.
 *  - If it is RETURN_MISSING, evaluator will return MISSING in the permissive mode.
 *  - in the LEGACY mode, the evaluator always throws exception irrespective of this flag.
 */
public enum class ErrorCode(
    public val category: ErrorCategory,
    private val properties: Set<Property>,
    private val messagePrefix: String,
    public val errorBehaviorInPermissiveMode: ErrorBehaviorInPermissiveMode = ErrorBehaviorInPermissiveMode.THROW_EXCEPTION
) {

    INTERNAL_ERROR(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "internal error"
    ),

    LEXER_INVALID_CHAR(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid character at"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    LEXER_INVALID_TOKEN(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid token"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    LEXER_INVALID_LITERAL(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid literal at"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenString(errorContext)
    },

    PARSE_INVALID_QUERY(
        ErrorCategory.PARSER,
        setOf(),
        "Invalid query syntax"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return "Invalid query syntax."
        }
    },

    PARSE_MALFORMED_PARSE_TREE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Internal error - malformed parse tree detected"
    ),

    PARSE_EXPECTED_NUMBER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Expected number, found"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenValue(errorContext)
    },

    PARSE_UNSUPPORTED_LITERALS_GROUPBY(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported literal in GROUP BY"
    ),

    PARSE_EXPECTED_DATE_TIME_PART(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected one of: [${ErrDateTimePart.values().joinToString()}]"
    ),

    PARSE_FAILED_STACK_OVERFLOW(
        ErrorCategory.PARSER,
        setOf(),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return "Statement too large. Parse failed due to stack overflow."
        }
    },

    PARSE_UNEXPECTED_TOKEN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected token found"
    ),

    PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Type parameter has exceeded the maximum allowed value of ${Int.MAX_VALUE}"
    ),

    PARSE_INVALID_PRECISION_FOR_TIME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid precision used for TIME type"
    ),

    // TODO: We should combine this with the above
    PARSE_INVALID_PRECISION_FOR_TIMESTAMP(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid precision used for TIMESTAMP type"
    ),

    PARSE_INVALID_DATE_STRING(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected date string to be of the format YYYY-MM-DD"
    ),

    PARSE_INVALID_TIME_STRING(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected time string to be of the format HH:MM:SS[.dddd...][+|-HH:MM]"
    ),

    PARSE_INVALID_DATETIME_STRING(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Invalid timestamp string"
    ),

    PARSE_INVALID_TRIM_SPEC(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Invalid arguments for trim"
    ),

    // Evaluator errors
    // TODO:  replace uses of this with UNIMPLEMENTED_FEATURE
    EVALUATOR_FEATURE_NOT_SUPPORTED_YET(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.FEATURE_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Feature '${errorContext?.get(Property.FEATURE_NAME)?.stringValue() ?: UNKNOWN}' not supported yet"
    },

    EVALUATOR_COUNT_DISTINCT_STAR(
        ErrorCategory.EVALUATOR,
        LOCATION,
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "COUNT(DISTINCT *) is not supported"
    },

    EVALUATOR_BINDING_DOES_NOT_EXIST(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME),
        "Binding does not exist",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Binding '${errorContext?.get(Property.BINDING_NAME)?.stringValue() ?: UNKNOWN}' does not exist"
    },

    EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME),
        "Binding does not exist. $UNBOUND_QUOTED_IDENTIFIER_HINT",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Binding '${errorContext?.get(Property.BINDING_NAME)?.stringValue() ?: UNKNOWN}' does not exist"
    },

    EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Variable '${errorContext?.get(Property.BINDING_NAME)?.stringValue() ?: UNKNOWN}' " +
                "must appear in the GROUP BY clause or be used in an aggregation function"
    },

    EVALUATOR_UNBOUND_PARAMETER(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_PARAMETER_ORDINAL, Property.BOUND_PARAMETER_COUNT),
        "No parameter bound for position!"
    ),

    EVALUATOR_INVALID_CAST(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.CAST_TO, Property.CAST_FROM),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Cannot convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
    },

    EVALUATOR_INVALID_CAST_NO_LOCATION(
        ErrorCategory.EVALUATOR,
        setOf(Property.CAST_TO, Property.CAST_FROM),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Cannot convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
    },

    EVALUATOR_CAST_FAILED(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.CAST_TO, Property.CAST_FROM),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Failed to convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
    },

    EVALUATOR_CAST_FAILED_NO_LOCATION(
        ErrorCategory.EVALUATOR,
        setOf(Property.CAST_TO, Property.CAST_FROM),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Failed to convert ${errorContext?.get(Property.CAST_FROM)?.stringValue() ?: UNKNOWN} " +
                "to ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN}"
    },

    EVALUATOR_NO_SUCH_FUNCTION(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.FUNCTION_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such function: ${errorContext?.get(Property.FUNCTION_NAME)?.stringValue() ?: UNKNOWN} "
    },

    EVALUATOR_ORDER_BY_NULL_COMPARATOR(
        ErrorCategory.EVALUATOR,
        LOCATION,
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String = ""
    },

    EVALUATOR_ENVIRONMENT_CANNOT_BE_RESOLVED(
        ErrorCategory.EVALUATOR,
        LOCATION,
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String = ""
    },

    EVALUATOR_NO_SUCH_PROCEDURE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.PROCEDURE_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such stored procedure: ${errorContext?.get(Property.PROCEDURE_NAME)?.stringValue() ?: UNKNOWN} "
    },

    EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.FUNCTION_NAME, Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX, Property.ACTUAL_ARITY),
        "Incorrect number of arguments to function call"
    ),

    EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX),
        "Incorrect number of arguments to procedure call"
    ),

    EVALUATOR_DATE_FIELD_OUT_OF_RANGE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Date field out of range."
    ),

    EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(
            Property.FUNCTION_NAME,
            Property.EXPECTED_ARGUMENT_TYPES,
            Property.ACTUAL_ARGUMENT_TYPES,
            Property.ARGUMENT_POSITION
        ),
        "Incorrect type of arguments to function call",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid argument type for ${errorContext?.get(Property.FUNCTION_NAME) ?: UNKNOWN} " +
                "argument number ${errorContext?.get(Property.ARGUMENT_POSITION) ?: UNKNOWN}, " +
                "expected: [${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN}] " +
                "got: ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES) ?: UNKNOWN}"
    },

    SEMANTIC_PROBLEM(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.MESSAGE),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return errorContext?.get(Property.MESSAGE)?.stringValue() ?: UNKNOWN
        }
    },

    EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.EXPECTED_ARGUMENT_TYPES, Property.ACTUAL_ARGUMENT_TYPES, Property.FUNCTION_NAME),
        "Incorrect type of arguments to procedure call"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid argument types for ${errorContext?.get(Property.FUNCTION_NAME) ?: UNKNOWN}, " +
                "expected: ${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN} " +
                "got: ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES) ?: UNKNOWN}"
    },

    /**
     * NOTE: This is unused and may be removed!
     *
     * To remove the dep on ExprValue, we introduce the EXPECTED_ARGUMENT_TYPES argument
     *  Property.EXPECTED_ARGUMENT_TYPES -> ExprValueType.values().filter { it.isText }
     */
    EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_ARGUMENT_TYPES),
        "Incorrect type of arguments for operator '||'",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Incorrect type of arguments for operator '||', " +
                "expected: ${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN} " +
                "got ${errorContext?.get(Property.ACTUAL_ARGUMENT_TYPES)}"
    },

    EVALUATOR_INVALID_PRECISION_FOR_TIME(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "invalid precision used for TIME type"
    ),

    /**
     * This is a generic error wrapper for the DateTimeException thrown by Java's [java.time] when attempting to create
     * an instance of [java.time.LocalTime] or [java.time.OffsetTime] when the time field is out of range.
     * The exception is caught by [org.partiql.lang.eval.time.Time.Companion.of].
     */
    EVALUATOR_TIME_FIELD_OUT_OF_RANGE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Invalid value for TIME type"
    ),

    /**
     * This is a generic error thrown whenever Java's [java.time.format.DateTimeFormatter] throws an exception when attempting to
     * parse a timestamp.  Ideally, this doesn't happen and the invalidity is detected by
     * [org.partiql.lang.eval.builtins.timestamp.FormatPattern] instead.  This needs to stick around until we
     * replace [java.time.format.DateTimeFormatter].
     */
    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        // TODO: Remove this enum after replacing [java.time.format.DateTimeFormatter].
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid timestamp format pattern: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains invalid token: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains invalid symbol: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_UNTERMINATED_TIMESTAMP_FORMAT_PATTERN_TOKEN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern contains unterminated token: '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}'."
    },

    EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN, Property.TIMESTAMP_FORMAT_PATTERN_FIELDS),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Timestamp format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' " +
                "requires additional fields '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN_FIELDS)}'."
    },

    EVALUATOR_TIMESTAMP_FORMAT_PATTERN_DUPLICATE_FIELDS(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN, Property.TIMESTAMP_FORMAT_PATTERN_FIELDS),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains multiple format " +
                "specifiers representing the timestamp field '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN_FIELDS)}'."
    },

    EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains a 12-hour hour of " +
                "day format symbol but doesn't also contain an AM/PM field, or it contains a 24-hour hour of day format " +
                "specifier and contains an AM/PM field."
    },

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL_FOR_PARSING(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "The format pattern '${errorContext?.get(Property.TIMESTAMP_FORMAT_PATTERN)}' contains a valid format " +
                "symbol that cannot be applied to timestamp parsing."
    },

    EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Failed to parse Ion timestamp",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "Failed to parse custom timestamp using the specified format pattern",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "loss of precision when parsing timestamp",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INTEGER_OVERFLOW(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.INT_SIZE_IN_BYTES),
        "Int overflow or underflow",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "INT-${errorContext?.get(Property.INT_SIZE_IN_BYTES) ?: UNKNOWN} overflow or underflow"
    },

    EVALUATOR_AMBIGUOUS_BINDING(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.BINDING_NAME, Property.BINDING_NAME_MATCHES),
        "Binding name was ambiguous",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Binding name was '${errorContext?.get(Property.BINDING_NAME)}'"
    },

    EVALUATOR_LIKE_INVALID_INPUTS(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.LIKE_VALUE, Property.LIKE_PATTERN, Property.LIKE_ESCAPE),
        "Invalid argument given to LIKE expression"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Given: " +
                "value = ${errorContext?.get(Property.LIKE_VALUE)?.stringValue() ?: UNKNOWN}, " +
                "pattern =  ${errorContext?.get(Property.LIKE_PATTERN)?.stringValue() ?: UNKNOWN}, " +
                "escape char = ${errorContext?.get(Property.LIKE_ESCAPE)?.stringValue() ?: "none given"}"
    },

    EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.LIKE_PATTERN, Property.LIKE_ESCAPE),
        "Pattern contains an invalid or malformed escape sequence"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Given: " +
                "pattern =  ${errorContext?.get(Property.LIKE_PATTERN)?.stringValue() ?: UNKNOWN}, " +
                "escape char = ${errorContext?.get(Property.LIKE_ESCAPE)?.stringValue() ?: "none given"}"
    },

    EVALUATOR_NON_INT_LIMIT_VALUE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_TYPE),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "LIMIT value must be an integer but found ${errorContext.getProperty(Property.ACTUAL_TYPE)}}"
    },

    EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_TYPE),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Struct field key should be text but found ${errorContext.getProperty(Property.ACTUAL_TYPE)}}."
    },

    EVALUATOR_NEGATIVE_LIMIT(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "LIMIT must not be negative"
    ),

    EVALUATOR_NON_INT_OFFSET_VALUE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_TYPE),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "OFFSET value must be an integer but found ${errorContext.getProperty(Property.ACTUAL_TYPE)}"
    },

    EVALUATOR_NEGATIVE_OFFSET(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "OFFSET must not be negative"
    ),

    EVALUATOR_DIVIDE_BY_ZERO(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "/ by zero",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_MODULO_BY_ZERO(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "% by zero",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_CONVERSION(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Invalid conversion",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_UNEXPECTED_VALUE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Unexpected value"
    ),

    EVALUATOR_UNEXPECTED_VALUE_TYPE(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Unexpected value type",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM(
        ErrorCategory.EVALUATOR,
        setOf(),
        "Invalid arguments for trim"
    ),

    EVALUATOR_TIMESTAMP_OUT_OF_BOUNDS(
        ErrorCategory.EVALUATOR,
        setOf(),
        "Timestamp out of bounds",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        setOf(),
        "Invalid arguments for function call",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART(
        ErrorCategory.EVALUATOR,
        setOf(),
        "Invalid arguments for date",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION(
        ErrorCategory.EVALUATOR,
        setOf(),
        "Invalid arguments for agg function"
    ),

    EVALUATOR_INVALID_COMPARISION(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Invalid comparision",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_INVALID_BINDING(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Invalid binding"
    ),

    EVALUATOR_ARITHMETIC_EXCEPTION(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Arithmetic exception",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    EVALUATOR_COUNT_START_NOT_ALLOWED(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "COUNT(*) not allowed"
    ),

    EVALUATOR_GENERIC_EXCEPTION(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Generic exception"
    ),

    EVALUATOR_VALUE_NOT_INSTANCE_OF_EXPECTED_TYPE(
        ErrorCategory.EVALUATOR,
        LOCATION + Property.EXPECTED_STATIC_TYPE,
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Value was not an instance of the expected static type: ${errorContext.getProperty(Property.EXPECTED_STATIC_TYPE)}"
    },

    EVALUATOR_NON_SINGLETON_COLLECTION(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Expected collection cannot be coerced to a single value",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ),

    SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.ACTUAL_TYPE),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Struct field key should be text but found ${errorContext.getProperty(Property.ACTUAL_TYPE)}}."
    },

    SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Global variable access is illegal in this context, variable name: '${errorContext.getProperty(Property.BINDING_NAME)}'"
    },

    SEMANTIC_UNBOUND_BINDING(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such variable named '${errorContext.getProperty(Property.BINDING_NAME)}'."
    },

    SEMANTIC_UNBOUND_QUOTED_BINDING(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such variable named '${errorContext.getProperty(Property.BINDING_NAME)}'. $UNBOUND_QUOTED_IDENTIFIER_HINT"
    },

    SEMANTIC_AMBIGUOUS_BINDING(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.BINDING_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "A variable named '${errorContext.getProperty(Property.BINDING_NAME)}' was already defined in this scope"
    },

    SEMANTIC_INVALID_DECIMAL_ARGUMENTS(
        ErrorCategory.SEMANTIC,
        LOCATION,
        "Invalid precision or scale for decimal"
    ),

    SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "HAVING cannot be used without GROUP BY or GROUP ALL"
    ),

    SEMANTIC_MISSING_AS_NAME(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Missing AS name"
    ),

    SEMANTIC_LITERAL_INT_OVERFLOW(
        ErrorCategory.SEMANTIC,
        LOCATION,
        "Literal int overflow or underflow"
    ),

    SEMANTIC_FLOAT_PRECISION_UNSUPPORTED(
        ErrorCategory.SEMANTIC,
        LOCATION,
        "FLOAT precision not supported"
    ),

    SEMANTIC_UNION_TYPE_INVALID(
        ErrorCategory.SEMANTIC,
        LOCATION,
        "Union type not permitted"
    ),

    PARSE_EXPECTED_WINDOW_ORDER_BY(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Expect ORDER BY in window specification"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return "Expect ORDER BY in window specification"
        }
    },

    // Generic errors
    UNIMPLEMENTED_FEATURE(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.FEATURE_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Feature '${errorContext?.get(Property.FEATURE_NAME)?.stringValue() ?: UNKNOWN}' not implemented yet"
    };

    protected fun getTokenString(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_STRING)?.stringValue() ?: UNKNOWN

    protected fun getTokenValue(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_VALUE)?.ionValue()?.toString() ?: UNKNOWN

    protected fun getTokenDescription(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.TOKEN_DESCRIPTION)?.toString() ?: UNKNOWN

    protected fun getTokenDescriptionAndTokenValue(errorContext: PropertyValueMap?): String =
        getTokenDescription(errorContext) + " : " + getTokenValue(errorContext)

    protected open fun detailMessagePrefix(): String = messagePrefix

    protected open fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
        getTokenDescriptionAndTokenValue(errorContext)

    /**
     * Given an [errorContext] generate a detailed error message.
     *
     * Template method.
     *
     * @param errorContext  that contains information about the error
     * @return detailed error message as a [String]
     */
    public open fun getErrorMessage(errorContext: PropertyValueMap?): String =
        "${detailMessagePrefix()}, ${detailMessageSuffix(errorContext)}"

    public fun getProperties(): Set<Property> = properties
}

private fun PropertyValueMap?.getProperty(prop: Property): String =
    this?.get(prop)?.toString() ?: UNKNOWN

// duplicated from internal org.partiql.lang.syntax.impl to remove circular dependency
private enum class ErrDateTimePart {
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE;
}
