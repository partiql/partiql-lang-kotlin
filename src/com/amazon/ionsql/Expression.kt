/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

/** An expression that can be evaluated to [IonValue]. */
interface Expression {
    /** Evaluates the expression with the given global name bindings. */
    fun eval(globals: Bindings): ExprValue
}
