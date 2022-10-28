package org.partiql.lang.eval.physical.window

internal fun createBuiltinWindowFunctions() =
    listOf(
        Lag(),
        Lead()
    )
