/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** A simple mapping of name to [ExprValue]. */
interface Bindings {
    companion object {
        /** Binds over an [ExprValue], which surfaces the `$value` name as itself. */
        fun over(value: ExprValue): Bindings = over {
            when (it) {
                "\$value" -> value
                else -> null
            }
        }

        /**
         * A SAM conversion for [Bindings] from a function object.
         *
         * This is necessary as Kotlin currently doesn't support SAM conversions to
         * Kotlin defined interfaces (only Java defined interfaces).
         */
        fun over(func: (String) -> ExprValue?): Bindings = object : Bindings {
            override fun get(name: String): ExprValue? = func(name)
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