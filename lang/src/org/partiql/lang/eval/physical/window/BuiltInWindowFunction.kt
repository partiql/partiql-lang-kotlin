package org.partiql.lang.eval.physical.window

@OptIn(Experimental::class)
internal fun createBuiltinWindowFunction(name: String) =
    when (name) {
        "lag" -> Lag()
        "lead" -> Lead()
        else -> error("Window function $name has not been implemented")
    }
