package org.partiql.lang.eval.physical.window

import org.partiql.annotations.ExperimentalWindowFunctions

@ExperimentalWindowFunctions
internal fun createBuiltinWindowFunction(name: String) =
    when (name) {
        "lag" -> Lag()
        "lead" -> Lead()
        else -> error("Window function $name has not been implemented")
    }

@ExperimentalWindowFunctions
internal fun createBuiltinWindowFunctionAsync(name: String) =
    when (name) {
        "lag" -> LagAsync()
        "lead" -> LeadAsync()
        else -> error("Window function $name has not been implemented")
    }
