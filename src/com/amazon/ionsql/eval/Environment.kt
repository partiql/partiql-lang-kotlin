/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * The environment for execution.
 *
 * @param globals The global bindings.
 * @param locals The current local bindings.
 * @param current The current bindings to use for evaluation which is generally
 *                `globals` or `locals` depending on the context.
 */
data class Environment(internal val globals: Bindings,
                       internal val locals: Bindings,
                       val current: Bindings = locals) {
    /** Constructs a new nested environment with the locals being the [current] bindings. */
    internal fun nest(newLocals: Bindings, useAsCurrent: Boolean = true): Environment {
        val derivedLocals = newLocals.delegate(locals)
        val newCurrent = when {
            useAsCurrent -> derivedLocals
            else -> current
        }
        return copy(locals = derivedLocals, current = newCurrent)
    }

    /** Constructs a copy of this environment wit the locals being the current bindings. */
    internal fun flipToLocals(): Environment = copy(current = locals)

    /** Constructs a copy of this environment with the [globals] being the current bindings. */
    internal fun flipToGlobals(): Environment = copy(current = globals)
}
