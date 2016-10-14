/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

/**
 * Provides an [ExprValue] over a function that yields a [Sequence].
 * This implementation is used to implement lazy sequences of values and is the
 * closest analog to the `Dataset` concept in Ion SQL.  Materialzing the
 * `ionValue` field generates an [IonList] from the sequence **once**.
 *
 * The Given [sequence] function **must** be stable, meaning each subsequent invocation
 * returns the same sequence of values otherwise the behavior of this value is *undefined*.
 *
 * @param ion The underlying [IonSystem] for generating values.
 * @param sequence The [Sequence] generating function.
 */
class SequenceExprValue(private val ion: IonSystem,
                        private val sequence: () -> Sequence<ExprValue>) : ExprValue {
    override val ionValue: IonValue by lazy {
        asSequence()
            .mapTo(ion.newEmptyList()) { it.ionValue.clone() }
            .seal()
    }

    override fun bind(parent: Bindings): Bindings = Bindings.over(this)

    override fun iterator(): Iterator<ExprValue> = sequence().iterator()
}
