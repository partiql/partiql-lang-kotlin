/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Represents a set of internal compiler slots for intermediate execution state.
 */
class RegisterBank(size: Int) {
    private val bank: Array<Register> = Array(size) { Register.EMPTY }

    /** Retrieves the register stored at the given index. */
    operator fun get(index: Int) = bank[index]

    /** Stores the given [ExprAggregator] into the given register index. */
    operator fun set(index: Int, aggregator: ExprAggregator) {
        bank[index] = object : Register() {
            override val aggregator = aggregator
        }
    }
}
