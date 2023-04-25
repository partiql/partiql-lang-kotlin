package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.builtins.ExprFunctionCurrentUser
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom

/**
 * Replaces all system function nodes with their appropriate function call.
 *
 * Currently supports:
 * - CURRENT_USER
 */
internal object SystemFunctionsVisitorTransform : VisitorTransformBase() {
    override fun transformExprSessionAttribute(node: PartiqlAst.Expr.SessionAttribute): PartiqlAst.Expr = PartiqlAst.build {
        val functionName = when (node.value.text.toUpperCase()) {
            EvaluationSession.Constants.CURRENT_USER_KEY -> ExprFunctionCurrentUser.FUNCTION_NAME
            else -> err(
                "Unsupported session attribute: ${node.value.text}",
                errorCode = ErrorCode.SEMANTIC_PROBLEM,
                errorContext = errorContextFrom(node.metas),
                internal = false
            )
        }
        call(
            funcName = functionName,
            args = emptyList()
        )
    }
}
