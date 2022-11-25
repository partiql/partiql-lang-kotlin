package org.partiql.lang.eval.physical.window

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
@ExperimentalWindowFunc
internal fun createBuiltinWindowFunction(name: String) =
    when (name) {
        "lag" -> Lag()
        "lead" -> Lead()
        else -> error("Window function $name has not been implemented")
    }
