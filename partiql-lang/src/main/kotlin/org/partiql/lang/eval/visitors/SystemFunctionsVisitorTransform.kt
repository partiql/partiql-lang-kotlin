package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.builtins.ExprFunctionCurrentUser

/**
 * Replaces all system function nodes with their appropriate function call.
 *
 * Currently supports:
 * - CURRENT_USER
 */
internal object SystemFunctionsVisitorTransform : VisitorTransformBase() {
    override fun transformExprCurrentUser(node: PartiqlAst.Expr.CurrentUser): PartiqlAst.Expr = PartiqlAst.build {
        call(
            funcName = ExprFunctionCurrentUser.NAME,
            args = emptyList()
        )
    }
}
