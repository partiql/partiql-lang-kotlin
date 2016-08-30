/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem

/**
 * Parses a list of tokens as infix query expression into prefix s-expression.
 */
class InfixParser(val ion: IonSystem) {
    fun parse(tokens: List<Token>): IonSexp = throw UnsupportedOperationException("FIXME!")
}