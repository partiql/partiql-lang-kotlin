package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical

/**
 * Simple API that defines a method to convert a [PartiqlPhysical.Expr] to a [PhysicalPlanThunkAsync].
 *
 * Intended to prevent [PhysicalBexprToThunkConverterAsync] from having to take a direct dependency on
 * [org.partiql.lang.eval.EvaluatingCompiler].
 */
internal interface PhysicalPlanCompilerAsync {
    suspend fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunkAsync
}
