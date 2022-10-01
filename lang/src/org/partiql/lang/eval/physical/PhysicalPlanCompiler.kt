package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical

/**
 * Simple API that defines a method to convert a [PartiqlPhysical.Expr] to a [PhysicalPlanThunk].
 *
 * Intended to prevent [PhysicalBexprToThunkConverter] from having to take a direct dependency on
 * [org.partiql.lang.eval.EvaluatingCompiler].
 */
internal interface PhysicalPlanCompiler {
    fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunk
}
