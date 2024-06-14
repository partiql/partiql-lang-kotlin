package org.partiql.planner.metadata

import org.partiql.value.PartiQLValueType

/**
 * A short list of builtins for testing
 */
val BUILTINS = listOf(
    Fn.Base(
        name = "foo",
        parameters = listOf(
            Fn.Parameter("x", PartiQLValueType.INT32),
            Fn.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
    Fn.Base(
        name = "bar",
        parameters = listOf(
            Fn.Parameter("x", PartiQLValueType.INT32),
            Fn.Parameter("y", PartiQLValueType.INT32),
        ),
        returnType = PartiQLValueType.BOOL
    ),
)
