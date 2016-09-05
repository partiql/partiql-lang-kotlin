/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

/**
 * A lazily evaluated [ExprValue] that represents a value with path components that
 * are functional transforms over the logical sequence of values starting at the root.
 *
 * @param root The value from whence the path starts.
 * @param components The transforms on the root to yield the eventual sequence of values.
 *                   Each element in this list can be thought of as a path element as
 *                   a functional transformation over the sequence of [IonValue]
 */
class PathExprValue(private val ion: IonSystem,
                    private val root: ExprValue,
                    private val components: List<(ExprValue) -> Sequence<ExprValue>>) : ExprValue {
    override val ionValue: IonValue
        get() = iterator()
            .asSequence()
            .mapTo(ion.newDatagram()) { it.ionValue.clone() }
            .apply { makeReadOnly() }

    override fun bind(parent: Bindings): Bindings = object : Bindings {
        override fun get(name: String): ExprValue? = when (name) {
            "\$value" -> this@PathExprValue
            else -> null
        }
    }

    override fun iterator(): Iterator<ExprValue> {
        var seq = root.iterator().asSequence()
        for (component in components) {
            seq = seq. flatMap(component)
        }
        return seq.iterator()
    }
}