/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem

/**
 * A basic implementation of the [Compiler] that parses Ion SQL into an [Expression].
 *
 * In particular, this implementation parses a SQL-like syntax over Ion s-expressions
 * by treating the s-expressions as an effective token stream.
 */
class InfixCompiler(private val ion: IonSystem) : Compiler {
    override fun compile(source: String): Expression {
        // We have to wrap the source in an s-expression to get the right parsing behavior
        val expression = ion.singleValue("($source)")

        throw UnsupportedOperationException("FIXME!")
    }
}