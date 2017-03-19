/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.util.*
import com.amazon.ionsql.eval.ExprValueType.*

/**
 * Provides an [ExprValue] over a function that yields a [Sequence].
 * This implementation is used to implement lazy sequences of values and is the
 * closest analog to the `Dataset` concept in Ion SQL.  Materialzing the
 * `ionValue` field generates an [IonList] from the sequence **once**.
 *
 * @param ion The underlying [IonSystem] for generating values.
 * @param sequence The [Sequence] generating function.
 */
class SequenceExprValue(private val ion: IonSystem,
                        override val type: ExprValueType,
                        private val sequence: Sequence<ExprValue>) : BaseExprValue() {

    constructor(ion: IonSystem, sequence: Sequence<ExprValue>) : this(ion, BAG, sequence)

    init {
        if (!type.isSequence) {
            err("Cannot bind non-sequence type to sequence: $type")
        }
    }

    override val ionValue: IonValue by lazy {
        asSequence()
            .mapTo(
                when (type) {
                    BAG, LIST -> ion.newEmptyList()
                    SEXP -> ion.newEmptySexp()
                    else -> throw IllegalStateException("Invalid type: $type")
                }
            ) { it.ionValue.clone() }
            .seal()
    }

    override val bindings = Bindings.empty()

    override fun iterator() = sequence.iterator()
}
