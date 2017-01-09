/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/**
 * Parses query expression source into an s-expression based abstract syntax tree.
 */
interface Lexer {
    fun tokenize(source: String): List<Token>
}
