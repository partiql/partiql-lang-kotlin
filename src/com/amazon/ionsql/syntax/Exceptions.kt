/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ionsql.IonSqlException

private fun genMessage(message: String, position: SourcePosition?): String {
    val positionMsg = when (position) {
        null -> "<UNKNOWN POSITION>"
        else -> position.toString()
    }
    return "$message at $positionMsg"
}

/** Root exception type for syntactic problems. */
open class SyntaxException(message: String,
                           val position: SourcePosition?,
                           cause: Throwable? = null)
    : IonSqlException(genMessage(message, position), cause)

/** Error in the Lexer. */
class LexerException(message: String,
                     position: SourcePosition?,
                     cause: Throwable? = null)
    : SyntaxException(message, position, cause)

/** Error in the parser. */
class ParserException(message: String,
                      position: SourcePosition?,
                      cause: Throwable? = null)
    : SyntaxException(message, position, cause)
