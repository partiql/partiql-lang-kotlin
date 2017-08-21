/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Variant type over elements of a [RegisterBank].
 *
 * Currently this supports registers that are [ExprAggregator], but may be expanded in the future
 * to support other things like [ExprValue] for things like optimized local variable access.
 */
abstract class Register {
    companion object {
        /** The empty register. */
        val EMPTY = object : Register() {}
    }

    /** The [ExprAggregator] value storedin this register. */
    open val aggregator: ExprAggregator
        get() = throw UnsupportedOperationException("Register is not an aggregator")
}
