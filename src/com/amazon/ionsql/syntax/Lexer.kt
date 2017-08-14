/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ionsql.errorhandling.IErrorHandler

/**
 * Tokenizes a [String] representation of a query into a [List] of [Token].
 *
 * Implementations must be thread-safe.
 */
interface Lexer {
    fun tokenize(source: String): List<Token>
    fun tokenize(source: String, errorHandler: IErrorHandler): List<Token>
}
