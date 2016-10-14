/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** A simple mapping of name to [ExprValue]. */
interface Bindings {
    companion object {
        private val EMPTY by lazy { over { null } }

        /** The empty bindings. */
        fun empty(): Bindings = EMPTY

        /** Binds over an [ExprValue], which surfaces the `$value` name as itself. */
        fun over(value: ExprValue): Bindings = over {
            when (it) {
                SYS_VALUE -> value
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

    /**
     * Wraps a binding with a set of names that should not be resolved to anything.
     *
     * @param bindings The bindings to delegate over.
     * @param names, the blacklisted names
     */
    fun blacklist(vararg names: String): Bindings {
        val blacklisted = names.toSet()
        return over {
            when (it) {
                in blacklisted -> null
                else -> get(it)
            }
        }
    }

    /**
     * Wraps these [Bindings] to delegate lookup to another instance when lookup on this
     * one fails.
     *
     * Note that this doesn't modify an existing [Bindings] but creates a new instance that
     * does delegation.
     *
     * @param fallback The bindings to delegate to when lookup fails to find a name.
     */
    fun delegate(fallback: Bindings): Bindings = over {
        val binding = this@Bindings[it]
        binding ?: fallback[it]
    }
}
