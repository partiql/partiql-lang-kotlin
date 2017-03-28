/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/** A simple mapping of ordinal index to [ExprValue]. */
interface OrdinalBindings {
    companion object {
        private val EMPTY = over { null }

        /** The empty bindings. */
        fun empty(): OrdinalBindings = EMPTY

        /**
         * A SAM conversion for [Bindings] from a function object.
         *
         * This is necessary as Kotlin currently doesn't support SAM conversions to
         * Kotlin defined interfaces (only Java defined interfaces).
         */
        fun over(func: (Int) -> ExprValue?): OrdinalBindings = object : OrdinalBindings {
            override fun get(index: Int): ExprValue? = func(index)
        }
    }

    /**
     * Looks up an index within this binding.
     *
     * @param index The binding to look up.  The index is zero-based.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     */
    operator fun get(index: Int): ExprValue?
}
