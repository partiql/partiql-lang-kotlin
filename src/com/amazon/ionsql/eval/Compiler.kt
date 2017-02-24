/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Compiles query expression source into an [Expression].
 * This interface makes no requirements over what the *syntax* is for the
 * expression, just that it compiles to an reified object that can be evaluated.
 */
interface Compiler {
    fun compile(source: String): Expression
}
