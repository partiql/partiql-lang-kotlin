/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

/**
 * Representation of a result of an [Expression].
 */
interface ExprValue : Iterable<ExprValue> {
    /**
     * Materializes the expression value as an [IonValue].
     *
     * The returned value may or may not be tethered to a container, so it is
     * the callers responsibility to deal with that accordingly (e.g. via `clone`).
     */
    val ionValue: IonValue

    /**
     * Creates [Bindings] over this result.
     *
     * This is generally used for operations that scope over the *result*
     * of some computation.
     *
     * @param parent The parent to delegate to.
     */
    fun bind(parent: Bindings): Bindings

    /**
     * Iterates over this value.
     *
     * If the underlying value is an *aggregate* type, this is a simple delegation.
     * If the underlying value is a *scalar* type, this produces a singleton.
     */
    operator override fun iterator(): Iterator<ExprValue>
}