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

package org.partiql.lang.errors

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.syntax.DATE_TIME_PART_KEYWORDS
import org.partiql.lang.syntax.TokenType

/** Property Set constants used in [ErrorCode] */
private val LOCATION = setOf(Property.LINE_NUMBER, Property.COLUMN_NUMBER)
private val TOKEN_INFO = setOf(Property.TOKEN_DESCRIPTION, Property.TOKEN_VALUE)
private val LOC_TOKEN = LOCATION + (TOKEN_INFO)
private val LOC_TOKEN_STR = LOCATION + (setOf(Property.TOKEN_STRING))

/** Helper function to reduce syntactical overhead of accessing property values as strings. */
private fun PropertyValueMap.getAsString(key: Property, defaultValue: String) =
    this[key]?.toString() ?: defaultValue

enum class ErrorBehaviorInPermissiveMode {
    THROW_EXCEPTION, RETURN_MISSING
}

internal const val UNBOUND_QUOTED_IDENTIFIER_HINT =
    "Hint: did you intend to use single quotes (') here instead of double quotes (\")? " +
        "Use single quotes (') for string literals and double quotes (\") for quoted identifiers."

/** Each [ErrorCode] contains an immutable set of [Property].
 *  These are the properties used as keys in [PropertyValueMap] created at each error location.
 *  @property errorBehaviorInPermissiveMode This enum is used during evaluation to determine the behavior of the error.
 *  - If it is THROW_EXCEPTION, which is the default behavior, evaluator will throw an EvaluationException in the permissive mode.
 *  - If is is RETURN_MISSING, evaluator will return MISSING in the permissive mode.
 *  - in the LEGACY mode, the evaluator always throws exception irrespective of this flag.
 */
enum class ErrorCode(
    internal val category: ErrorCategory,
    private val properties: Set<Property>,
    private val messagePrefix: String,
    val errorBehaviorInPermissiveMode: ErrorBehaviorInPermissiveMode = ErrorBehaviorInPermissiveMode.THROW_EXCEPTION
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

    LEXER_INVALID_NAME(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid name"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    LEXER_INVALID_OPERATOR(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid operator at"
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    LEXER_INVALID_ION_LITERAL(
        ErrorCategory.LEXER,
        LOC_TOKEN_STR,
        "invalid ion literal at"
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.KEYWORD),
        "expected keyword"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getKeyword(errorContext)
    },

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_TOKEN_TYPE(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.EXPECTED_TOKEN_TYPE),
        "expected token of type"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            errorContext?.get(Property.EXPECTED_TOKEN_TYPE)?.tokenTypeValue()?.toString() ?: UNKNOWN +
                "found ${getTokenDescription(errorContext)}"
    },

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_2_TOKEN_TYPES(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.EXPECTED_TOKEN_TYPE_1_OF_2, Property.EXPECTED_TOKEN_TYPE_2_OF_2),
        "unexpected token"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            "expected ${errorContext?.getAsString(Property.EXPECTED_TOKEN_TYPE_1_OF_2, UNKNOWN)}" +
                " or ${errorContext?.getAsString(Property.EXPECTED_TOKEN_TYPE_2_OF_2, UNKNOWN)}" +
                " but found ${getTokenDescription(errorContext)}"
    },

    PARSE_EXPECTED_NUMBER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Expected number, found"
    ) {
        override fun detailMessageSuffix(errorContext: PropertyValueMap?): String =
            getTokenValue(errorContext)
    },

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_TYPE_NAME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected type name, found"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_WHEN_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected WHEN clause in CASE"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_WHERE_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected WHERE clause"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_CONFLICT_ACTION(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected <conflict action>"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_RETURNING_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected <returning mapping>"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_RETURNING_CLAUSE_SYNTAX(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported syntax in RETURNING clause"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_TOKEN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Unexpected token"
    ),

    PARSE_UNSUPPORTED_LITERALS_GROUPBY(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported literal in GROUP BY"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_MEMBER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected MEMBER node"
    ),

    PARSE_EXPECTED_DATE_TIME_PART(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected one of: [${DATE_TIME_PART_KEYWORDS.joinToString()}]"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_SELECT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported use of SELECT"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_CASE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported use of CASE"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_CASE_CLAUSE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Unsupported use of CASE statement"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_ALIAS(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported syntax for alias, `at` and `as` are supported"
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_SYNTAX(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported Syntax"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNKNOWN_OPERATOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unsupported operator"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_INVALID_PATH_COMPONENT(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.TOKEN_TYPE, Property.TOKEN_VALUE),
        "invalid Path component"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String {
            return "Invalid path component, expecting either an ${TokenType.IDENTIFIER} or ${TokenType.STAR}, " +
                "got: ${errorContext?.get(Property.TOKEN_TYPE) ?: UNKNOWN} " +
                "with value: ${errorContext?.get(Property.TOKEN_VALUE) ?: UNKNOWN}"
        }
    },

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_MISSING_IDENT_AFTER_AT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "identifier expected after `@` symbol"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNEXPECTED_OPERATOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected operator"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNEXPECTED_TERM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected term found"
    ),

    PARSE_UNEXPECTED_TOKEN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected token found"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNEXPECTED_KEYWORD(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "unexpected keyword found"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_EXPRESSION(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected expression"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis after CAST"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected right parenthesis"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_ARGUMENT_DELIMITER(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected argument delimiter"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_CAST_ARITY(
        ErrorCategory.PARSER,
        LOC_TOKEN + setOf(Property.CAST_TO, Property.EXPECTED_ARITY_MIN, Property.EXPECTED_ARITY_MAX),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Cast to type ${errorContext?.get(Property.CAST_TO)?.stringValue() ?: UNKNOWN} has incorrect arity." +
                "Correct arity is ${errorContext?.get(Property.EXPECTED_ARITY_MIN)?.integerValue() ?: UNKNOWN}.." +
                "${errorContext?.get(Property.EXPECTED_ARITY_MAX)?.integerValue() ?: UNKNOWN}"
    },

    PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Type parameter has exceeded the maximum allowed value of ${Int.MAX_VALUE}"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_INVALID_TYPE_PARAM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid value used for type parameter"
    ),

    PARSE_INVALID_PRECISION_FOR_TIME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "invalid precision used for TIME type"
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EMPTY_SELECT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "found empty SELECT list"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_SELECT_MISSING_FROM(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "missing FROM after SELECT list"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_MISSING_OPERATION(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected DML or SELECT operation after FROM"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_MISSING_SET_ASSIGNMENT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected assignment for SET"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_IDENT_FOR_GROUP_NAME(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for GROUP name"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_IDENT_FOR_ALIAS(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for alias"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_KEYWORD_FOR_MATCH(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected keyword for match"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_IDENT_FOR_MATCH(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for match"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_LEFT_PAREN_FOR_MATCH_NODE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left parenthesis for match node"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_RIGHT_PAREN_FOR_MATCH_NODE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected right parenthesis for match node"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_LEFT_BRACKET_FOR_MATCH_EDGE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected left bracket for match edge"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_RIGHT_BRACKET_FOR_MATCH_EDGE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected right bracket for match edge"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_PARENTHESIZED_PATTERN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected appropriate closing punctuation for parenthesized pattern"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_EDGE_PATTERN_MATCH_EDGE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected edge pattern for match edge"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_EQUALS_FOR_MATCH_PATH_VARIABLE(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected equals for match path variable"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_AS_FOR_LET(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected AS for LET clause"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_UNSUPPORTED_CALL_WITH_STAR(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "function call, other than COUNT, with (*) as parameter is not supported"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Aggregate function calls take 1 argument only"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_NO_STORED_PROCEDURE_PROVIDED(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "No stored procedure provided"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_MALFORMED_JOIN(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "malformed use of FROM with JOIN"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_EXPECTED_IDENT_FOR_AT(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "expected identifier for AT name"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Invalid use of * in select list"
    ),

    // SQB = SQuare Bracket
    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_CANNOT_MIX_SQB_AND_WILDCARD_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOC_TOKEN,
        "Cannot mix [] and * in the same expression in a select list"
    ),

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST(
        ErrorCategory.PARSER,
        LOCATION,
        "Other expressions may not be present in the select list when '*' is used without dot notation."
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    SEMANTIC_DUPLICATE_ALIASES_IN_SELECT_LIST_ITEM(
        ErrorCategory.SEMANTIC,
        LOCATION,
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Duplicate projection field encountered in SelectListItem expression"
    },

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    SEMANTIC_NO_SUCH_FUNCTION(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.FUNCTION_NAME),
        ""
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "No such function: ${errorContext?.get(Property.FUNCTION_NAME)?.stringValue() ?: UNKNOWN} "
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

    @Deprecated("This ErrorCode is subject to removal.") // To be removed before 1.0
    @Suppress("UNUSED")
    SEMANTIC_INCORRECT_ARGUMENT_TYPES_TO_FUNC_CALL(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.EXPECTED_ARGUMENT_TYPES, Property.ACTUAL_ARGUMENT_TYPES, Property.FUNCTION_NAME),
        "Incorrect type of arguments to function call"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Invalid argument types for ${errorContext?.get(Property.FUNCTION_NAME) ?: UNKNOWN}, " +
                "expected: ${errorContext?.get(Property.EXPECTED_ARGUMENT_TYPES) ?: UNKNOWN} " +
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

    EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.ACTUAL_ARGUMENT_TYPES),
        "Incorrect type of arguments for operator '||'",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Incorrect type of arguments for operator '||', " +
                "expected one of ${ExprValueType.values().filter { it.isText }} " +
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
     * This is a generic error thrown whenever Java's [DateTimeFormatter] throws an exception when attempting to
     * parse a timestamp.  Ideally, this doesn't happen and the invalidity is detected by
     * [org.partiql.lang.eval.builtins.timestamp.FormatPattern] instead.  This needs to stick around until we
     * replace [DateTimeFormatter].
     * TODO:  remove this after replacing [DateTimeFormatter].
     */
    EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN(
        ErrorCategory.EVALUATOR,
        LOCATION + setOf(Property.TIMESTAMP_FORMAT_PATTERN),
        "",
        ErrorBehaviorInPermissiveMode.RETURN_MISSING
    ) {
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
        override fun getErrorMessage(errorContext: PropertyValueMap?) =
            "Value was not an instance of the expected static type: ${errorContext.getProperty(Property.EXPECTED_STATIC_TYPE)}"
    },

    EVALUATOR_NON_TEXT_STRUCT_KEY(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "STRUCT key must be text",
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

    /**
     * Indicates incorrectness surrounding arity of [NAry] and [DataType] nodes.
     */
    @Deprecated("This ErrorCode is subject to removal.")
    @Suppress("UNUSED")
    SEMANTIC_INCORRECT_NODE_ARITY(
        ErrorCategory.SEMANTIC,
        LOCATION + setOf(Property.EXPECTED_ARITY_MAX, Property.EXPECTED_ARITY_MIN, Property.ACTUAL_ARITY, Property.FUNCTION_NAME),
        "Incorrect number of arguments for node"
    ) {
        override fun getErrorMessage(errorContext: PropertyValueMap?): String =
            "Incorrect number of arguments supplied to `${errorContext.getProperty(Property.FUNCTION_NAME)}`. " +
                "Min = ${errorContext.getProperty(Property.EXPECTED_ARITY_MIN)}, max = ${errorContext.getProperty(Property.EXPECTED_ARITY_MAX)} " +
                "Actual = ${errorContext.getProperty(Property.ACTUAL_ARITY)}"
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

    SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS(
        ErrorCategory.EVALUATOR,
        LOCATION,
        "`*` may not be used with other items in a select list"
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

    protected fun getKeyword(errorContext: PropertyValueMap?): String =
        errorContext?.get(Property.KEYWORD)?.stringValue() ?: UNKNOWN

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
    open fun getErrorMessage(errorContext: PropertyValueMap?): String =
        "${detailMessagePrefix()}, ${detailMessageSuffix(errorContext)}"

    fun errorCategory(): String = category.toString()

    fun getProperties(): Set<Property> = properties
}

private fun PropertyValueMap?.getProperty(prop: Property): String =
    this?.get(prop)?.toString() ?: UNKNOWN
