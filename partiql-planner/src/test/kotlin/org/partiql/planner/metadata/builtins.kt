package org.partiql.planner.metadata

import org.partiql.value.PartiQLValueType

/**
 * A short list of builtins for testing
 */
val BUILTINS = listOf(
    Function.Base(
        name = "foo",
        parameters = listOf(
            Function.Parameter("x", PartiQLValueType.INT32),
            Function.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
    Function.Base(
        name = "bar",
        parameters = listOf(
            Function.Parameter("x", PartiQLValueType.INT32),
            Function.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
)
