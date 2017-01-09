/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ionsql.TokenType.*

/** All SQL-92 keywords. */
val SQL92_KEYWORDS = sortedSetOf(
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

val BOOLEAN_KEYWORDS = sortedSetOf("true", "false")

/** Operator renames for the AST. */
val OPERATOR_ALIASES = mapOf(
    "!=" to "<>"
)

/**
 * Binary operators not including set operators.
 * Set operators have interesting grammar implications (e.g. UNION ALL)
 */
val BINARY_OPERATORS = sortedSetOf(
    "+", "-", "/", "%", "*",
    "<", "<=", ">", ">=", "=", "<>",
    "||",
    "and", "or"
)

val UNARY_OPERATORS = sortedSetOf(
    "+", "-", "not", "@"
)

val ALL_OPERATORS = BINARY_OPERATORS union UNARY_OPERATORS

/**
 * Precedence rank integer is ascending with higher precedance and is in terms of the
 * un-aliased names of the operators.
 */
val OPERATOR_PRECEDENCE = mapOf(
    "or"  to 1,
    "and" to 2,

    // equality group (TODO add other morphemes of equality/non-equality)
    "="   to 3,
    "<>"  to 3,

    // comparison group
    "<"   to 4,
    "<="  to 4,
    ">"   to 4,
    ">="  to 4,

    // the addition group
    "+"   to 5,
    "-"   to 5,
    "||"  to 5,

    // multiply group (TODO add exponentiation)
    "*"   to 6,
    "/"   to 6,
    "%"   to 6,

    // lexical scope operator
    "@"   to 7
)

//
// Character Classes
// Strings as place holders for immutable character arrays
//

private fun allCase(chars: String) = chars.toLowerCase() + chars.toUpperCase()

val SIGN_CHARS = "+-"

val NON_ZERO_DIGIT_CHARS = "123456789"
val DIGIT_CHARS = "0" + NON_ZERO_DIGIT_CHARS
val HEXDIGIT_CHARS = DIGIT_CHARS + allCase("ABCDEF")

val E_NOTATION_CHARS = allCase("E")
val HEX_NOTATION_CHARS = allCase("X")
val BIN_NOTATION_CHARS = allCase("B")

val NON_OVERLOADED_OPERATOR_CHARS = "^!%<>=|@"
val OPERATOR_CHARS = NON_OVERLOADED_OPERATOR_CHARS + "+-*/"

val ALPHA_CHARS = allCase("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
val IDENT_START_CHARS = "_\$" + ALPHA_CHARS
val IDENT_CONTINUE_CHARS = IDENT_START_CHARS + DIGIT_CHARS

val NL_WHITESPACE_CHARS = "\u000D\u000A"                 // CR, LF
val NON_NL_WHITESPACE_CHARS = "\u0009\u000B\u000C\u0020" // TAB, VT, FF, SPACE
val ALL_WHITESPACE_CHARS = NL_WHITESPACE_CHARS + NON_NL_WHITESPACE_CHARS

val DOUBLE_QUOTE_CHARS = "\""
val SINGLE_QUOTE_CHARS = "'"
val BACKTICK_CHARS = "`"
