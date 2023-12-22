package org.partiql.planner.internal.typer

import org.partiql.plugin.PartiQLFunctions

internal object FnBuiltins {

    /**
     * Static PartiQL casts information.
     */
    @JvmStatic
    val pCasts = TypeCasts.partiql()

    /**
     * Static PartiQL function signatures, don't recompute.
     */
    @JvmStatic
    val pFns = PartiQLFunctions.functions.toFnMap()

    /**
     * Static PartiQL operator signatures, don't recompute.
     */
    @JvmStatic
    val pOps = (PartiQLFunctions.operators + pCasts.relationships().map { it.castFn }).toFnMap()

    /*
     * Static PartiQL aggregation signatures, don't recompute.
     */
    @JvmStatic
    val pAggs = PartiQLFunctions.aggregations.toFnMap()
}
