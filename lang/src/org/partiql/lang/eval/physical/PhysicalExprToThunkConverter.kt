package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ThunkEnv

/**
 * Simple API that defines a method to convert a [PartiqlPhysical.Expr] to a [ThunkEnv].
 *
 * Intended to abstract prevent [PhysicalBexprToThunkConverter] from having to take a direct dependency on
 * [org.partiql.lang.eval.EvaluatingCompiler].
 */
internal interface PhysicalExprToThunkConverter {
    fun convert(expr: PartiqlPhysical.Expr): ThunkEnv
}
