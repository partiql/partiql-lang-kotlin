package org.partiql.planner.metadata

import org.partiql.value.PartiQLValueType

/**
 * A short list of builtins for testing
 */
val BUILTINS = listOf(
    Routine.Base(
        name = "foo",
        parameters = listOf(
            Routine.Parameter("x", PartiQLValueType.INT32),
            Routine.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
    Routine.Base(
        name = "bar",
        parameters = listOf(
            Routine.Parameter("x", PartiQLValueType.INT32),
            Routine.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
)
