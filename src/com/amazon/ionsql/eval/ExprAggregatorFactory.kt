/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Simple functional interface to create [ExprAggregator] instances.
 *
 * This is the entry point for *SQL* aggregate function definitions in the evaluator.
 */
interface ExprAggregatorFactory {
    companion object {
        fun over(func: () -> ExprAggregator): ExprAggregatorFactory =
            object : ExprAggregatorFactory {
                override fun create() = func()
            }
    }

    /** Generates a new instance of an aggregator. */
    fun create(): ExprAggregator
}
