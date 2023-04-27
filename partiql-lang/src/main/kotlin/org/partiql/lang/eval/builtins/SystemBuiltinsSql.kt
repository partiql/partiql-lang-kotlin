package org.partiql.lang.eval.builtins

import com.amazon.ion.IonType
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

internal val SYSTEM_BUILTINS_SQL = listOf<ExprFunction>(
    ExprFunctionCurrentUser
)

internal object ExprFunctionCurrentUser : ExprFunction {
    internal const val FUNCTION_NAME: String = "\$__current_user"

    override val signature: FunctionSignature = FunctionSignature(
        name = FUNCTION_NAME,
        requiredParameters = emptyList(),
        returnType = StaticType.unionOf(StaticType.STRING, StaticType.NULL)
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue = when (val user = session.context[EvaluationSession.Constants.CURRENT_USER_KEY]) {
        is String -> ExprValue.newString(user)
        null -> ExprValue.newNull(IonType.STRING)
        else -> err(
            message = "CURRENT_USER must be either a STRING or NULL.",
            errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE,
            errorContext = errorContextFrom(emptyMetaContainer()),
            internal = false
        )
    }
}
