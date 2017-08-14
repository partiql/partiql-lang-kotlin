/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ion.IonSexp
import com.amazon.ionsql.errorhandling.IErrorHandler

/**
 * Parses a list of [Token] into an [IonSexp] based AST.
 *
 * Implementations must be thread-safe.
 */
interface Parser {
    fun parse(source: String): IonSexp
    fun parse(source: String, errorHandler: IErrorHandler): IonSexp
}
