/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

/**
 * Representation of a result of an [Expression].
 */
interface ExpressionValue : Iterable<ExpressionValue> {
    /**
     * Materializes the expression value as an [IonValue].
     *
     * The requirement of implementations is that the resulting value is untethered.
     * This almost certainly implies `clone`.
     */
    fun ionize(): IonValue

    /**
     * Creates [Bindings] over this result.
     *
     * This is generally used for operations that scope over the *result*
     * of some computation.
     *
     * @param parent The parent to delegate to.
     */
    fun bindOver(parent: Bindings): Bindings

    /**
     * Iterates over this value.
     *
     * If the underlying value is an *aggregate* type, this is a simple delegation.
     * If the underlying value is a *scalar* type, this produces a singleton.
     */
    operator override fun iterator(): Iterator<ExpressionValue>
}