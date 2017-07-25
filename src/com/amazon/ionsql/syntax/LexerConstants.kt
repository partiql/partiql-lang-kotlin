/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ionsql.syntax.TokenType.*

/** All SQL-92 keywords. */
internal val SQL92_KEYWORDS = setOf(
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
    "both",
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
    "create",
    "cross",
    "current",
    "current_date",
    "current_time",
    "current_timestamp",
    "current_user",
    "cursor",
    "date",
    "day",
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
    "hour",
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
    "leading",
    "left",
    "level",
    "like",
    "local",
    "lower",
    "match",
    "max",
    "min",
    "minute",
    "module",
    "month",
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
    "second",
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
    "timezone_hour",
    "timezone_minute",
    "to",
    "trailing",
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
    "year",
    "zone"
)

/** Ion SQL++ additional keywords. */
internal val IONSQL_KEYWORDS = setOf(
    "missing",
    "pivot",
    "unpivot",
    "limit",
    "tuple",

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
internal val KEYWORDS = SQL92_KEYWORDS union IONSQL_KEYWORDS

/** Keywords that are aliases for type keywords. */
internal val TYPE_ALIASES = mapOf(
    "varchar"   to "character_varying",
    "char"      to "character",
    "dec"       to "decimal",
    "int"       to "integer",
    "bool"      to "boolean"
)

/** Keywords that are purely aliases to other keywords. */
internal val KEYWORD_ALIASES = TYPE_ALIASES

/**
 * Indicates the keywords (and pseudo keywords) the indicate types.
 * Some of these types (e.g. VARCHAR) requires a parameters, but many implementations
 * don't require that.
 */
internal val TYPE_NAME_ARITY_MAP = mapOf(
    "missing"           to 0..0, // IonSQL++
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
    "character"         to 0..1, // SQL-92
    "character_varying" to 0..1, // SQL-92
    "string"            to 0..0, // Ion
    "symbol"            to 0..0, // Ion
    "clob"              to 0..0, // Ion
    "blob"              to 0..0, // Ion
    "struct"            to 0..0, // Ion
    "tuple"             to 0..0, // IonSQL++
    "list"              to 0..0, // Ion
    "sexp"              to 0..0, // Ion
    "bag"               to 0..0  // IonSQL++
    // TODO SQL-92 types BIT, BIT VARYING, DATE, TIME, INTERVAL and TIMEZONE qualifier
)

/** Keywords that are normal function names. */
internal val FUNCTION_NAME_KEYWORDS = setOf(
    "exists",

    // aggregate functions
    // COUNT has special syntax
    "avg",
    "max",
    "min",
    "sum",

    // string functions
    // POSITION, SUBSTRING, TRIM, EXTRACT, TRANSLATE, CONVERT have special syntax
    "char_length",
    "character_length",
    "octet_length",
    "bit_length",
    "upper",
    "lower",

    // conditionals
    "nullif",
    "coalesce"
)

internal val BOOLEAN_KEYWORDS = setOf("true", "false")

/** Operator renames for the AST. */
internal val OPERATOR_ALIASES = mapOf(
    "!=" to "<>"
)

/** Operators that parse as infix, but have special parsing rules. */
internal val SPECIAL_INFIX_OPERATORS = setOf(
    "between", "not_between",
    "like", "not_like"        // optionally a ternary operator when `ESCAPE` is present
)

/** Binary operators with verbatim lexical token equivalents. */
internal val SINGLE_LEXEME_BINARY_OPERATORS = setOf(
    "+", "-", "/", "%", "*",
    "<", "<=", ">", ">=", "=", "<>",
    "||",
    "and", "or",
    "is", "in",
    "union", "except", "intersect"
)

/** Tokens comprising multiple lexemes (**happens before** keyword aliasing). */
internal val MULTI_LEXEME_TOKEN_MAP = mapOf(
    listOf("not", "in")                 to ("not_in" to OPERATOR),
    listOf("is", "not")                 to ("is_not" to OPERATOR),
    listOf("not", "between")            to ("not_between" to OPERATOR),
    listOf("intersect", "all")          to ("intersect_all" to OPERATOR),
    listOf("except", "all")             to ("except_all" to OPERATOR),
    listOf("union", "all")              to ("union_all" to OPERATOR),
    listOf("character", "varying")      to ("character_varying" to KEYWORD),
    listOf("double", "precision")       to ("double_precision" to KEYWORD),
    listOf("not", "like")               to ("not_like" to OPERATOR),
    listOf("cross", "join")             to ("inner_join" to KEYWORD),
    listOf("inner", "join")             to ("inner_join" to KEYWORD),
    listOf("left", "join")              to ("left_join" to KEYWORD),
    listOf("left", "outer", "join")     to ("left_join" to KEYWORD),
    listOf("right", "join")             to ("right_join" to KEYWORD),
    listOf("right", "outer", "join")    to ("right_join" to KEYWORD),
    listOf("full", "join")              to ("outer_join" to KEYWORD),
    listOf("outer", "join")             to ("outer_join" to KEYWORD),
    listOf("full", "outer", "join")     to ("outer_join" to KEYWORD)
)

internal val MULTI_LEXEME_MIN_LENGTH = MULTI_LEXEME_TOKEN_MAP.keys.map { it.size }.min()!!
internal val MULTI_LEXEME_MAX_LENGTH = MULTI_LEXEME_TOKEN_MAP.keys.map { it.size }.max()!!

internal val MULTI_LEXEME_BINARY_OPERATORS =
    MULTI_LEXEME_TOKEN_MAP.values.filter {
        it.second == TokenType.OPERATOR && it.first !in SPECIAL_INFIX_OPERATORS
    }.map { it.first }

/** Binary operators. */
internal val BINARY_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + MULTI_LEXEME_BINARY_OPERATORS

/** Unary operators. */
internal val UNARY_OPERATORS = setOf(
    "+", "-", "not"
)

/** All operators with special parsing rules. */
internal val SPECIAL_OPERATORS = SPECIAL_INFIX_OPERATORS + setOf(
    "@"
)

internal val ALL_SINGLE_LEXEME_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS
internal val ALL_OPERATORS =
    BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS

/**
 * Precedence rank integer is ascending with higher precedance and is in terms of the
 * un-aliased names of the operators.
 */
internal val INFIX_OPERATOR_PRECEDENCE = mapOf(
    // set operator group
    "intersect"     to 5,
    "intersect_all" to 5,
    "except"        to 5,
    "except_all"    to 5,
    "union"         to 5,
    "union_all"     to 5,

    // logical group
    "or"            to 10,
    "and"           to 20,

    // equality group (TODO add other morphemes of equality/non-equality)
    "="             to 30,
    "<>"            to 30,
    "is"            to 30,
    "is_not"        to 30,
    "in"            to 30,
    "not_in"        to 30,

    // comparison group
    "<"             to 40,
    "<="            to 40,
    ">"             to 40,
    ">="            to 40,
    "between"       to 40, // note that this **must** be above 'AND'
    "not_between"   to 40, // note that this **must** be above 'AND'
    "like"          to 40,
    "not_like"      to 40,

    // the addition group
    "+"             to 50,
    "-"             to 50,
    "||"            to 50,

    // multiply group (TODO add exponentiation)
    "*"             to 60,
    "/"             to 60,
    "%"             to 60
)

//
// Character Classes
// Strings as place holders for immutable character arrays
//

private fun allCase(chars: String) = chars.toLowerCase() + chars.toUpperCase()

internal val SIGN_CHARS = "+-"

internal val NON_ZERO_DIGIT_CHARS = "123456789"
internal val DIGIT_CHARS = "0" + NON_ZERO_DIGIT_CHARS

internal val E_NOTATION_CHARS = allCase("E")

internal val NON_OVERLOADED_OPERATOR_CHARS = "^%=@+"
internal val OPERATOR_CHARS = NON_OVERLOADED_OPERATOR_CHARS + "-*/<>|!"

internal val ALPHA_CHARS = allCase("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
internal val IDENT_START_CHARS = "_\$" + ALPHA_CHARS
internal val IDENT_CONTINUE_CHARS = IDENT_START_CHARS + DIGIT_CHARS

internal val NL_WHITESPACE_CHARS = "\u000D\u000A"                 // CR, LF
internal val NON_NL_WHITESPACE_CHARS = "\u0009\u000B\u000C\u0020" // TAB, VT, FF, SPACE
internal val ALL_WHITESPACE_CHARS = NL_WHITESPACE_CHARS + NON_NL_WHITESPACE_CHARS

internal val DOUBLE_QUOTE_CHARS = "\""
internal val SINGLE_QUOTE_CHARS = "'"
internal val BACKTICK_CHARS = "`"
