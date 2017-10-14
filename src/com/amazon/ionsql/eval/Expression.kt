/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonValue

/**
 * An expression that can be evaluated to [IonValue].
 */
interface Expression {

    /**
     * Evaluates the expression with the given Session
     */
    fun eval(session: EvaluationSession): ExprValue
}
