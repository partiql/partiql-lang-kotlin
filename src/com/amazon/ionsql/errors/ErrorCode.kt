package com.amazon.ionsql.errors

/** Property Set constants used in [ErrorCode] */
private val LOCATION = setOf(Property.LINE_NUMBER, Property.COLUMN_NUMBER)
private val TOKEN_INFO = setOf(Property.TOKEN_TYPE, Property.TOKEN_VALUE)
private val LOC_TOKEN = LOCATION + (TOKEN_INFO)
private val LOC_TOKEN_STR = LOCATION + (setOf(Property.TOKEN_STRING))


/** Helper function to reduce syntactical overhead of accessing property values as strings. */
private fun PropertyValueMap.getAsString(key: Property, defaultValue: String) =
        this[key]?.stringValue() ?: defaultValue

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
        LOC_TOKEN,
        "invalid Path component"),

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

    PARSE_EMPTY_SELECT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "found empty SELECT list"),

    PARSE_SELECT_MISSING_FROM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "missing FROM after SELECT list"),

    PARSE_EXPECTED_IDENT_FOR_GROUP_NAME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for GROUP name"),

    PARSE_EXPECTED_IDENT_FOR_ALIAS(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for alias"),

    PARSE_UNSUPPORTED_CALL_WITH_STAR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "function call, other than COUNT, with (*) as parameter is not supported"),

    PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Aggregate function calls take 1 argument only"),

    PARSE_MALFORMED_JOIN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "malformed use of FROM with JOIN"),

    PARSE_EXPECTED_IDENT_FOR_AT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for AT name"),

    //Evaluator errors

    EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "Incorrect number of arguments to function call"),

    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "invalid timestamp format pattern"),

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
        "loss of precision when parsing timestamp")
    ;

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