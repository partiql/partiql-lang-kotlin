/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonValue
import com.amazon.ion.facet.Faceted

/**
 * Representation of a value within the context of an [Expression].
 */
interface ExprValue : Iterable<ExprValue>, Faceted {
    /** The type of value independent of its implementation. */
    val type: ExprValueType

    /**
     * Materializes the expression value as an [IonValue].
     *
     * The returned value may or may not be tethered to a container, so it is
     * the callers responsibility to deal with that accordingly (e.g. via `clone`).
     */
    val ionValue: IonValue

    /** Returns the [Scalar] view of this value. */
    val scalar: Scalar

    /**
     * Returns the [Bindings] over this value.
     *
     * This is generally used to access a component of a value by name.
     */
    val bindings: Bindings

    /**
     * Returns the [OrdinalBindings] over this value.
     *
     * This is generally used to access a component of a value by index.
     */
    val ordinalBindings: OrdinalBindings

    /**
     * Iterates over this value's *child* elements.
     *
     * If this value has no children, then it should return the empty iterator.
     */
    operator override fun iterator(): Iterator<ExprValue>
}
