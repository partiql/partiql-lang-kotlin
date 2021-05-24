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

package org.partiql.lang.syntax

import org.partiql.lang.syntax.TokenType.KEYWORD
import org.partiql.lang.syntax.TokenType.OPERATOR

@JvmField internal val TRIM_SPECIFICATION_KEYWORDS = setOf("both", "leading", "trailing")

//TODO: Rename DatePart to DateTimePart. Check issue https://github.com/partiql/partiql-lang-kotlin/issues/409
internal enum class DatePart {
    YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE
}

internal val DATE_PART_KEYWORDS: Set<String> = DatePart.values()
    .map { it.toString().toLowerCase() }.toSet()

/** All SQL-92 keywords. */
@JvmField internal val SQL92_KEYWORDS = setOf(
    "absolute",
    "action",
    "add",
    "all",
    "allocate",
    "alter",
    "and",
    "any",
    "are",
    "as",
    "asc",
    "assertion",
    "at",
    "authorization",
    "avg",
    "begin",
    "between",
    "bit",
    "bit_length",
    "by",
    "cascade",
    "cascaded",
    "case",
    "cast",
    "catalog",
    "char",
    "character",
    "character_length",
    "char_length",
    "check",
    "close",
    "coalesce",
    "collate",
    "collation",
    "column",
    "commit",
    "connect",
    "connection",
    "constraint",
    "constraints",
    "continue",
    "convert",
    "corresponding",
    "count",
    "create",
    "cross",
    "current",
    "current_date",
    "current_time",
    "current_timestamp",
    "current_user",
    "cursor",
    "date",
    "deallocate",
    "dec",
    "decimal",
    "declare",
    "default",
    "deferrable",
    "deferred",
    "delete",
    "desc",
    "describe",
    "descriptor",
    "diagnostics",
    "disconnect",
    "distinct",
    "domain",
    "double",
    "drop",
    "else",
    "end",
    "end-exec",
    "escape",
    "except",
    "exception",
    "exec",
    "execute",
    "exists",
    "external",
    "extract",
    "date_add",
    "date_diff",
    "false",
    "fetch",
    "first",
    "float",
    "for",
    "foreign",
    "found",
    "from",
    "full",
    "get",
    "global",
    "go",
    "goto",
    "grant",
    "group",
    "having",
    "identity",
    "immediate",
    "in",
    "indicator",
    "initially",
    "inner",
    "input",
    "insensitive",
    "insert",
    "int",
    "integer",
    "intersect",
    "interval",
    "into",
    "is",
    "isolation",
    "join",
    "key",
    "language",
    "last",
    "left",
    "level",
    "like",
    "local",
    "lower",
    "match",
    "max",
    "min",
    "module",
    "names",
    "national",
    "natural",
    "nchar",
    "next",
    "no",
    "not",
    "null",
    "nullif",
    "numeric",
    "octet_length",
    "of",
    "on",
    "only",
    "open",
    "option",
    "or",
    "order",
    "outer",
    "output",
    "overlaps",
    "pad",
    "partial",
    "position",
    "precision",
    "prepare",
    "preserve",
    "primary",
    "prior",
    "privileges",
    "procedure",
    "public",
    "read",
    "real",
    "references",
    "relative",
    "restrict",
    "revoke",
    "right",
    "rollback",
    "rows",
    "schema",
    "scroll",
    "section",
    "select",
    "session",
    "session_user",
    "set",
    "size",
    "smallint",
    "some",
    "space",
    "sql",
    "sqlcode",
    "sqlerror",
    "sqlstate",
    "substring",
    "sum",
    "system_user",
    "table",
    "temporary",
    "then",
    "time",
    "timestamp",
    "to",
    "transaction",
    "translate",
    "translation",
    "trim",
    "true",
    "union",
    "unique",
    "unknown",
    "update",
    "upper",
    "usage",
    "user",
    "using",
    "value",
    "values",
    "varchar",
    "varying",
    "view",
    "when",
    "whenever",
    "where",
    "with",
    "work",
    "write",
    "zone"
)
// Note: DATE_PART_KEYWORDs are not keywords in the traditional sense--they are only keywords within
// the context of the DATE_ADD, DATE_DIFF and EXTRACT functions, for which [SqlParser] has special support.
// Similarly, TRIM_SPECIFICATION_KEYWORDS are only keywords within the context of the TRIM function.

/** PartiQL additional keywords. */
@JvmField internal val SQLPP_KEYWORDS = setOf(
    "missing",
    "pivot",
    "unpivot",
    "limit",
    "tuple",
    "remove",
    "index",
    "conflict",
    "do",
    "nothing",
    "returning",
    "modified",
    "all",
    "new",
    "old",
    "let",

    // Ion type names

    // null
    "bool",
    "boolean",
    // int
    // float
    // decimal
    // timestamp
    "string",
    "symbol",
    "clob",
    "blob",
    "struct",
    "list",
    "sexp",
    "bag"
)

/** All Keywords. */
@JvmField internal val KEYWORDS = SQL92_KEYWORDS union SQLPP_KEYWORDS

/** Keywords that are aliases for type keywords. */
@JvmField internal val TYPE_ALIASES = mapOf(
    "varchar"   to "character_varying",
    "char"      to "character",
    "dec"       to "decimal",
    "int"       to "integer",
    "bool"      to "boolean"
)

/** Keywords that are purely aliases to other keywords. */
@JvmField internal val KEYWORD_ALIASES = TYPE_ALIASES

/**
 * Indicates the keywords (and pseudo keywords) the indicate types.
 * Some of these types (e.g. VARCHAR) requires a parameters, but many implementations
 * don't require that.
 */
@JvmField internal val TYPE_NAME_ARITY_MAP = mapOf(
    "missing"           to 0..0, // PartiQL
    "null"              to 0..0, // Ion
    "boolean"           to 0..0, // Ion & SQL-99
    "smallint"          to 0..0, // SQL-92
    "integer"           to 0..0, // Ion & SQL-92
    "float"             to 0..1, // Ion & SQL-92
    "real"              to 0..1, // SQL-92
    "double_precision"  to 0..0, // SQL-92
    "decimal"           to 0..2, // Ion & SQL-92
    "numeric"           to 0..2, // SQL-92
    "timestamp"         to 0..0, // Ion & SQL-92
    "date"              to 0..0, // PartiQL & SQL-92
    "time"              to 0..1, // PartiQL & SQL-92
    "character"         to 0..1, // SQL-92
    "character_varying" to 0..1, // SQL-92
    "string"            to 0..0, // Ion
    "symbol"            to 0..0, // Ion
    "clob"              to 0..0, // Ion
    "blob"              to 0..0, // Ion
    "struct"            to 0..0, // Ion
    "tuple"             to 0..0, // PartiQL
    "list"              to 0..0, // Ion
    "sexp"              to 0..0, // Ion
    "bag"               to 0..0  // PartiQL
    // TODO SQL-92 types BIT, BIT VARYING, DATE, TIME, INTERVAL and TIMEZONE qualifier
)

/** Keywords that are normal function names. */
@JvmField internal val FUNCTION_NAME_KEYWORDS = setOf(
    "exists",

    // aggregate functions
    "count",
    "avg",
    "max",
    "min",
    "sum",

    // string functions
    // POSITION, SUBSTRING, TRIM, EXTRACT, TRANSLATE, CONVERT have special syntax
    "substring",
    "char_length",
    "character_length",
    "octet_length",
    "bit_length",
    "upper",
    "lower",

    // functions
    "size",

    // conditionals
    "nullif",
    "coalesce",

    // sexp/list/bag constructors as functions
    "sexp",
    "list",
    "bag"
)

/** Aggregates functions. */
@JvmField val STANDARD_AGGREGATE_FUNCTIONS = setOf(
    "count",
    "avg",
    "max",
    "min",
    "sum"
)

@JvmField internal val BASE_DML_KEYWORDS  = setOf("insert_into", "set", "remove")

@JvmField internal val BOOLEAN_KEYWORDS = setOf("true", "false")

/** Operator renames for the AST. */
@JvmField internal val OPERATOR_ALIASES = mapOf(
    "!=" to "<>"
)

/** Operators that parse as infix, but have special parsing rules. */
@JvmField internal val SPECIAL_INFIX_OPERATORS = setOf(
    "between", "not_between",
    "like", "not_like"        // optionally a ternary operator when `ESCAPE` is present
)

/** Binary operators with verbatim lexical token equivalents. */
@JvmField internal val SINGLE_LEXEME_BINARY_OPERATORS = setOf(
    "+", "-", "/", "%", "*",
    "<", "<=", ">", ">=", "=", "<>",
    "||",
    "and", "or",
    "is", "in",
    "union", "except", "intersect"
)

/** Tokens comprising multiple lexemes (**happens before** keyword aliasing). */
@JvmField internal val MULTI_LEXEME_TOKEN_MAP = mapOf(
    listOf("not", "in")                 to ("not_in" to OPERATOR),
    listOf("is", "not")                 to ("is_not" to OPERATOR),
    listOf("not", "between")            to ("not_between" to OPERATOR),
    listOf("intersect", "all")          to ("intersect_all" to OPERATOR),
    listOf("except", "all")             to ("except_all" to OPERATOR),
    listOf("union", "all")              to ("union_all" to OPERATOR),
    listOf("character", "varying")      to ("character_varying" to KEYWORD),
    listOf("double", "precision")       to ("double_precision" to KEYWORD),
    listOf("not", "like")               to ("not_like" to OPERATOR),
    listOf("cross", "join")             to ("cross_join" to KEYWORD),
    listOf("inner", "join")             to ("inner_join" to KEYWORD),
    listOf("inner", "cross", "join")    to ("cross_join" to KEYWORD),
    listOf("left", "join")              to ("left_join" to KEYWORD),
    listOf("left", "outer", "join")     to ("left_join" to KEYWORD),
    listOf("left", "cross", "join")     to ("left_cross_join" to KEYWORD),
    listOf("left", "outer",
           "cross", "join")             to ("left_cross_join" to KEYWORD),
    listOf("right", "join")             to ("right_join" to KEYWORD),
    listOf("right", "outer", "join")    to ("right_join" to KEYWORD),
    listOf("right", "cross", "join")    to ("right_cross_join" to KEYWORD),
    listOf("right", "outer",
           "cross", "join")             to ("right_cross_join" to KEYWORD),
    listOf("full", "join")              to ("outer_join" to KEYWORD),
    listOf("outer", "join")             to ("outer_join" to KEYWORD),
    listOf("full", "outer", "join")     to ("outer_join" to KEYWORD),
    listOf("full", "cross", "join")     to ("outer_cross_join" to KEYWORD),
    listOf("outer", "cross", "join")    to ("outer_cross_join" to KEYWORD),
    listOf("full", "outer",
           "cross", "join")             to ("outer_cross_join" to KEYWORD),
    listOf("insert", "into")            to ("insert_into" to KEYWORD),
    listOf("on", "conflict")            to ("on_conflict" to KEYWORD),
    listOf("do", "nothing")             to ("do_nothing" to KEYWORD),
    listOf("modified", "old")           to ("modified_old" to KEYWORD),
    listOf("modified", "new")           to ("modified_new" to KEYWORD),
    listOf("all", "old")                to ("all_old" to KEYWORD),
    listOf("all", "new")                to ("all_new" to KEYWORD)
)

@JvmField internal val MULTI_LEXEME_MIN_LENGTH = MULTI_LEXEME_TOKEN_MAP.keys.map { it.size }.min()!!
@JvmField internal val MULTI_LEXEME_MAX_LENGTH = MULTI_LEXEME_TOKEN_MAP.keys.map { it.size }.max()!!

@JvmField internal val MULTI_LEXEME_BINARY_OPERATORS =
    MULTI_LEXEME_TOKEN_MAP.values.filter {
        it.second == TokenType.OPERATOR && it.first !in SPECIAL_INFIX_OPERATORS
    }.map { it.first }

/** Binary operators. */
@JvmField internal val BINARY_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + MULTI_LEXEME_BINARY_OPERATORS

/** Unary operators. */
@JvmField internal val UNARY_OPERATORS = setOf(
    "+", "-", "not"
)

/** All operators with special parsing rules. */
@JvmField internal val SPECIAL_OPERATORS = SPECIAL_INFIX_OPERATORS + setOf(
    "@"
)

@JvmField internal val ALL_SINGLE_LEXEME_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS
@JvmField internal val ALL_OPERATORS =
    BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS

/**
 * Operator precedence groups
 */
enum class OperatorPrecedenceGroups(val precedence: Int) {
    SET(5),
    SELECT(6),
    LOGICAL_OR(10),
    LOGICAL_AND(20),
    LOGICAL_NOT(30),
    EQUITY(40),
    COMPARISON(50),
    ADDITION(60),
    MULTIPLY(70)
}

/**
 * Precedence rank integer is ascending with higher precedence and is in terms of the
 * un-aliased names of the operators.
 */
@JvmField internal val OPERATOR_PRECEDENCE = mapOf(
    // set operator group
    "intersect"     to OperatorPrecedenceGroups.SET.precedence,
    "intersect_all" to OperatorPrecedenceGroups.SET.precedence,
    "except"        to OperatorPrecedenceGroups.SET.precedence,
    "except_all"    to OperatorPrecedenceGroups.SET.precedence,
    "union"         to OperatorPrecedenceGroups.SET.precedence,
    "union_all"     to OperatorPrecedenceGroups.SET.precedence,

    // logical group
    "or"            to OperatorPrecedenceGroups.LOGICAL_OR.precedence,
    "and"           to OperatorPrecedenceGroups.LOGICAL_AND.precedence,
    "not"           to OperatorPrecedenceGroups.LOGICAL_NOT.precedence,

    // equality group (TODO add other morphemes of equality/non-equality)
    "="             to OperatorPrecedenceGroups.EQUITY.precedence,
    "<>"            to OperatorPrecedenceGroups.EQUITY.precedence,
    "is"            to OperatorPrecedenceGroups.EQUITY.precedence,
    "is_not"        to OperatorPrecedenceGroups.EQUITY.precedence,
    "in"            to OperatorPrecedenceGroups.EQUITY.precedence,
    "not_in"        to OperatorPrecedenceGroups.EQUITY.precedence,

    // comparison group
    "<"             to OperatorPrecedenceGroups.COMPARISON.precedence,
    "<="            to OperatorPrecedenceGroups.COMPARISON.precedence,
    ">"             to OperatorPrecedenceGroups.COMPARISON.precedence,
    ">="            to OperatorPrecedenceGroups.COMPARISON.precedence,
    "between"       to OperatorPrecedenceGroups.COMPARISON.precedence, // note that this **must** be above 'AND'
    "not_between"   to OperatorPrecedenceGroups.COMPARISON.precedence, // note that this **must** be above 'AND'
    "like"          to OperatorPrecedenceGroups.COMPARISON.precedence,
    "not_like"      to OperatorPrecedenceGroups.COMPARISON.precedence,

    // the addition group
    "+"             to OperatorPrecedenceGroups.ADDITION.precedence,
    "-"             to OperatorPrecedenceGroups.ADDITION.precedence,
    "||"            to OperatorPrecedenceGroups.ADDITION.precedence,

    // multiply group (TODO add exponentiation)
    "*"             to OperatorPrecedenceGroups.MULTIPLY.precedence,
    "/"             to OperatorPrecedenceGroups.MULTIPLY.precedence,
    "%"             to OperatorPrecedenceGroups.MULTIPLY.precedence
)

//
// Character Classes
// Strings as place holders for immutable character arrays
//

private fun allCase(chars: String) = chars.toLowerCase() + chars.toUpperCase()

const internal val SIGN_CHARS = "+-"

const internal val NON_ZERO_DIGIT_CHARS = "123456789"
const internal val DIGIT_CHARS = "0" + NON_ZERO_DIGIT_CHARS

@JvmField internal val E_NOTATION_CHARS = allCase("E")

const internal val NON_OVERLOADED_OPERATOR_CHARS = "^%=@+"
const internal val OPERATOR_CHARS = NON_OVERLOADED_OPERATOR_CHARS + "-*/<>|!"

@JvmField internal val ALPHA_CHARS = allCase("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
@JvmField internal val IDENT_START_CHARS = "_\$" + ALPHA_CHARS
@JvmField internal val IDENT_CONTINUE_CHARS = IDENT_START_CHARS + DIGIT_CHARS

const internal val NL_WHITESPACE_CHARS = "\u000D\u000A"                 // CR, LF
const internal val NON_NL_WHITESPACE_CHARS = "\u0009\u000B\u000C\u0020" // TAB, VT, FF, SPACE
const internal val ALL_WHITESPACE_CHARS = NL_WHITESPACE_CHARS + NON_NL_WHITESPACE_CHARS

const internal val DOUBLE_QUOTE_CHARS = "\""
const internal val SINGLE_QUOTE_CHARS = "'"
const internal val BACKTICK_CHARS = "`"
