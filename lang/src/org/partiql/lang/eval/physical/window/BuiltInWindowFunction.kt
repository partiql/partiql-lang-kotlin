package org.partiql.lang.eval.physical.window

@OptIn(Experimental::class)
internal fun createBuiltinWindowFunctions() =
    listOf(
        Lag(),
        Lead()
    )
