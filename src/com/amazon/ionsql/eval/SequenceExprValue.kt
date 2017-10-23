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
 * This implementation is used to implement lazy sequences of values.
 *
 * The [ExprValue.ionValue] property lazily generates an [IonList] from the sequence **once**.
 * The [ExprValue.ordinalBindings] property lazily generates a backing [List] **once**
 * for non-`BAG` types.
 *
 * @param ion The underlying [IonSystem] for generating values.
 * @param type The reported [ExprValueType] for this value.
 * @param sequence The [Sequence] generating function.
 */
class SequenceExprValue(private val ion: IonSystem,
                        override val type: ExprValueType,
                        private val sequence: Sequence<ExprValue>) : BaseExprValue() {

    constructor(ion: IonSystem, sequence: Sequence<ExprValue>) : this(ion, BAG, sequence)

    init {
        if (!type.isSequence) {
            errNoContext("Cannot bind non-sequence type to sequence: $type", internal = true)
        }
    }

    override val ionValue: IonValue by lazy {
        sequence
            .mapTo(
                when (type) {
                    BAG, LIST -> ion.newEmptyList()
                    SEXP -> ion.newEmptySexp()
                    else -> throw IllegalStateException("Invalid type: $type")
                }
            ) { it.ionValue.clone() }
            .seal()
    }

    override val ordinalBindings: OrdinalBindings by lazy {
        when (type) {
            // no ordinal access over BAG
            BAG -> OrdinalBindings.empty()
            else -> {
                // materialize the sequence as a backing list
                val list = toList()
                OrdinalBindings.over {
                    list[it]
                }
            }
        }
    }

    override fun iterator() = sequence.iterator()
}
