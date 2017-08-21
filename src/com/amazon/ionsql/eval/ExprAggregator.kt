/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

// TODO consider making this API stateless and purely functional, AVG makes this ugly

/**
 * Defines an *SQL* aggregate function in the evaluator in terms of a stateful accumulator.
 * An aggregate function is always unary, and effectively operates over a collection
 * (e.g. `BAG`/`LIST`) of values.  This API defines the accumulator function over elements of the
 * operand.  The evaluator's responsibility is to effectively compile this definition
 * into a form of [ExprFunction] that operates over the collection as an [ExprValue].
 */
interface ExprAggregator {
    /** Accumulates the next value into this [ExprAggregator]. */
    fun next(value: ExprValue)

    /** Digests the result of the accumulated values. */
    fun compute(): ExprValue
}
