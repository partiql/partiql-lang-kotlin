/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ionsql.IonSqlException
import com.amazon.ionsql.errorhandling.ErrorCode
import com.amazon.ionsql.errorhandling.PropertyBag



/** Root exception type for syntactic problems. */
open class SyntaxException(message: String = "",
                           errorCode: ErrorCode,
                           errorContext: PropertyBag,
                           cause: Throwable? = null)
    : IonSqlException(message, errorCode, errorContext, cause)

/** Error in the Lexer. */
open class LexerException(message: String = "",
                          errorCode: ErrorCode,
                          errorContext: PropertyBag,
                          cause: Throwable? = null)
    : SyntaxException(message, errorCode, errorContext, cause)

/** Error in the parser. */
open class ParserException(message: String = "",
                           errorCode: ErrorCode,
                           errorContext: PropertyBag,
                           cause: Throwable? = null)
    : SyntaxException(message, errorCode, errorContext, cause)