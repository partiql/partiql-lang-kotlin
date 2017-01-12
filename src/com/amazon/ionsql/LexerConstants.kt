/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** All SQL-92 keywords. */
internal val SQL92_KEYWORDS = sortedSetOf(
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

/** All Keywords. */
val KEYWORDS = SQL92_KEYWORDS union sortedSetOf(
    "pivot",
    "unpivot",
    "limit"
)

internal val BOOLEAN_KEYWORDS = sortedSetOf("true", "false")

/** Operator renames for the AST. */
internal val OPERATOR_ALIASES = mapOf(
    "!=" to "<>"
)

/** Binary operators with verbatim lexical token equivalents. */
internal val SINGLE_LEXEME_BINARY_OPERATORS = sortedSetOf(
    "+", "-", "/", "%", "*",
    "<", "<=", ">", ">=", "=", "<>",
    "||",
    "and", "or",
    "is",
    "like",
    "union", "except", "intersect"
)

/** Binary operators comprising two lexemes. */
internal val DOUBLE_LEXEME_BINARY_OPERATOR_MAP = mapOf(
    ("is" to "not")     to "is_not",
    ("union" to "all")  to "union_all"
)

/** Binary operators. */
internal val BINARY_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + DOUBLE_LEXEME_BINARY_OPERATOR_MAP.values

/** Unary operators. */
internal val UNARY_OPERATORS = sortedSetOf(
    "+", "-", "not", "@"
)

/** Operators with special parsing rules. */
internal val SPECIAL_OPERATORS = sortedSetOf(
    "between"
)

val ALL_SINGLE_LEXEME_OPERATORS =
    SINGLE_LEXEME_BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS
val ALL_OPERATORS =
    BINARY_OPERATORS + UNARY_OPERATORS + SPECIAL_OPERATORS

/**
 * Precedence rank integer is ascending with higher precedance and is in terms of the
 * un-aliased names of the operators.
 */
internal val OPERATOR_PRECEDENCE = mapOf(
    // set operator group
    "intersect" to 5,
    "except"    to 5,
    "union"     to 5,
    "union_all" to 5,

    // logical group
    "or"        to 10,
    "and"       to 20,

    // equality group (TODO add other morphemes of equality/non-equality)
    "="         to 30,
    "<>"        to 30,
    "is"        to 30,
    "is_not"    to 30,

    // comparison group
    "<"         to 40,
    "<="        to 40,
    ">"         to 40,
    ">="        to 40,
    "between"   to 40, // note that this **must** be above 'AND'
    "like"      to 40,

    // the addition group
    "+"         to 50,
    "-"         to 50,
    "||"        to 50,

    // multiply group (TODO add exponentiation)
    "*"         to 60,
    "/"         to 60,
    "%"         to 60,

    // lexical scope operator
    "@"         to 70
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

internal val NON_OVERLOADED_OPERATOR_CHARS = "^!%<>=|@"
internal val OPERATOR_CHARS = NON_OVERLOADED_OPERATOR_CHARS + "+-*/"

internal val ALPHA_CHARS = allCase("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
internal val IDENT_START_CHARS = "_\$" + ALPHA_CHARS
internal val IDENT_CONTINUE_CHARS = IDENT_START_CHARS + DIGIT_CHARS

internal val NL_WHITESPACE_CHARS = "\u000D\u000A"                 // CR, LF
internal val NON_NL_WHITESPACE_CHARS = "\u0009\u000B\u000C\u0020" // TAB, VT, FF, SPACE
internal val ALL_WHITESPACE_CHARS = NL_WHITESPACE_CHARS + NON_NL_WHITESPACE_CHARS

internal val DOUBLE_QUOTE_CHARS = "\""
internal val SINGLE_QUOTE_CHARS = "'"
internal val BACKTICK_CHARS = "`"
