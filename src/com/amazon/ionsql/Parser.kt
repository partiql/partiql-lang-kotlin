/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp

/**
 * Parses a list of [Token] into an [IonSexp] based AST.
 */
interface Parser {
    fun parse(tokens: List<Token>): IonSexp
}
