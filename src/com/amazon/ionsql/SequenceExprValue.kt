/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

// TODO consolidate this with ExpandedExprValue

/**
 * Provides an [ExprValue] over a function that yields a [Sequence].
 */
class SequenceExprValue(private val ion: IonSystem,
                        private val sequence: () -> Sequence<ExprValue>) : ExprValue {
    override val ionValue: IonValue
        get() = asSequence()
            .mapTo(ion.newEmptyList()) { it.ionValue.clone() }
            .seal()

    override fun bind(parent: Bindings): Bindings = Bindings.over(this)

    override fun iterator(): Iterator<ExprValue> = sequence().iterator()
}