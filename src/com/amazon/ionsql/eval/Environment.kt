/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * The environment for execution.
 *
 * @param locals The current local bindings.
 * @param current The current bindings to use for evaluation which is generally
 *                `globals` or `locals` depending on the context.
 * @param session the evaluation session
 * @param registers The compiler specific *register* slots.
 */
data class Environment(internal val locals: Bindings,
                       val current: Bindings = locals,
                       val session: EvaluationSession,
                       val registers: RegisterBank) {

    internal enum class CurrentMode {
        LOCALS,
        GLOBALS_THEN_LOCALS
    }

    /** Constructs a new nested environment with the locals being the [current] bindings. */
    internal fun nest(newLocals: Bindings, currentMode: CurrentMode = CurrentMode.LOCALS): Environment {
        val derivedLocals = newLocals.delegate(locals)
        val newCurrent = when (currentMode) {
            CurrentMode.LOCALS -> derivedLocals
            CurrentMode.GLOBALS_THEN_LOCALS -> session.globals.delegate(derivedLocals)
        }
        return copy(locals = derivedLocals, current = newCurrent)
    }

    /** Constructs a copy of this environment wit the locals being the current bindings. */
    internal fun flipToLocals(): Environment = copy(current = locals)

    /** Constructs a copy of this environment with the [globals] being the current bindings. */
    internal fun flipToGlobalsFirst(): Environment = copy(current = session.globals.delegate(locals))
}
