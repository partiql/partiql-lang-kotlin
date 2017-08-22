/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.errors.DefaultErrorHandler
import com.amazon.ionsql.errors.ErrorHandler

/**
 * Compiles query expression source into an [Expression].
 * This interface makes no requirements over what the *syntax* is for the
 * expression, just that it compiles to a reified object that can be evaluated.
 */
interface Compiler {
    fun compile(source: String): Expression
    /**
     * Compile query expression and delegate errors to [errorHandler]
     */
    fun compile(source: String, errorHandler: ErrorHandler = DefaultErrorHandler()): Expression
}
