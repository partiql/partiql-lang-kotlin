/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

// TODO consolidate this with ResultSetExprValue

/**
 * A lazily evaluated [ExprValue] that represents a value with path components that
 * are functional transforms over the logical sequence of values starting at the root.
 *
 * Values of this type necessarily "detach" from whatever context they were in when
 * evaluated as an [IonValue].  Thus for static paths (paths of single field/indices),
 * this class is not appropriate.
 *
 * @param ion The ion system to synthesize over.
 * @param root The value from whence the path starts.
 * @param components The transforms on the root to yield the eventual sequence of values.
 *                       Each element in this list can be thought of as a path element as
 *                       a functional transformation over the sequence of [IonValue]
 */
class ExpandedExprValue(private val ion: IonSystem,
                        private val root: ExprValue,
                        private val components: List<(ExprValue) -> Sequence<ExprValue>>) : ExprValue {
    override val ionValue: IonValue
        get() = asSequence()
            .mapTo(ion.newEmptyList()) { it.ionValue.clone() }
            .seal()

    override fun bind(parent: Bindings): Bindings = Bindings.over(this)

    override fun iterator(): Iterator<ExprValue> {
        var seq = root.asSequence()
        for (component in components) {
            seq = seq. flatMap(component)
        }
        return seq.iterator()
    }
}