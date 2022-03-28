/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import java.util.TreeMap

/**
 * The environment for execution.
 *
 * @param locals The current local bindings.
 * @param current The current bindings to use for evaluation which is generally
 *                `globals` or `locals` depending on the context.
 * @param session The evaluation session.
 * @param groups The map of [Group]s that is currently being built during query execution.
 */
internal data class Environment(
    internal val locals: Bindings<ExprValue>,
    val current: Bindings<ExprValue> = locals,
    val session: EvaluationSession,
    val groups: MutableMap<ExprValue, Group> = createGroupMap(),
    val currentGroup: Group? = null
) {

    companion object {
        fun standard() = Environment(locals = Bindings.empty(), session = EvaluationSession.standard())

        private fun createGroupMap() = TreeMap<ExprValue, Group>(DEFAULT_COMPARATOR)
    }

    internal enum class CurrentMode {
        LOCALS,
        GLOBALS_THEN_LOCALS
    }

    /** Constructs a new nested environment with the locals being the [current] bindings. */
    internal fun nest(
        newLocals: Bindings<ExprValue>,
        currentMode: CurrentMode = CurrentMode.LOCALS,
        newGroup: Group? = currentGroup
    ): Environment {

        val derivedLocals = newLocals.delegate(locals)
        val newCurrent = when (currentMode) {
            CurrentMode.LOCALS -> derivedLocals
            CurrentMode.GLOBALS_THEN_LOCALS -> session.globals.delegate(derivedLocals)
        }
        return copy(locals = derivedLocals, current = newCurrent, currentGroup = newGroup)
    }

    /**
     * Creates a new environment with the same [Bindings] and session but with empty grouping state.
     * This is what allows GROUP BY to work in sub-queries without running into any grouping state
     * from the outer query.
     */
    internal fun nestQuery() = copy(
        currentGroup = null,
        groups = createGroupMap()
    )

    /** Constructs a copy of this environment with the locals being the current bindings. */
    internal fun flipToLocals(): Environment = copy(current = locals)

    /** Constructs a copy of this environment with the [globals] being the current bindings. */
    internal fun flipToGlobalsFirst(): Environment = copy(current = session.globals.delegate(locals))
}
