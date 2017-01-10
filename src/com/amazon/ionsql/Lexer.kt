/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/**
 * Tokenizes a [String] representation of a query into a [List] of [Token].
 */
interface Lexer {
    fun tokenize(source: String): List<Token>
}
