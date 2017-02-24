/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/** A simple mapping of name to [ExprValue]. */
interface Bindings {
    companion object {
        private val EMPTY = over { null }

        /** The empty bindings. */
        fun empty(): Bindings = EMPTY

        /**
         * A SAM conversion for [Bindings] from a function object.
         *
         * This is necessary as Kotlin currently doesn't support SAM conversions to
         * Kotlin defined interfaces (only Java defined interfaces).
         */
        fun over(func: (String) -> ExprValue?): Bindings = object : Bindings {
            override fun get(name: String): ExprValue? = func(name)
        }

        /** A [Bindings] instance with exactly one mapping. */
        fun singleton(name: String, value: ExprValue): Bindings = over {
            when (it) {
                name -> value
                else -> null
            }
        }
    }

    /**
     * Looks up a name within the environment.
     *
     * @param name The binding to look up.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     *         For the expression if a `null` **Ion** value is required, a non-`null`
     *         reference value for a `null` **must** be returned.
     */
    operator fun get(name: String): ExprValue?
}
