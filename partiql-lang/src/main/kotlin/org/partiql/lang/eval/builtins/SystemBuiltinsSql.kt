package org.partiql.lang.eval.builtins

import com.amazon.ion.IonType
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

internal val SYSTEM_BUILTINS_SQL = listOf<ExprFunction>(
    ExprFunctionCurrentUser
)

internal object ExprFunctionCurrentUser : ExprFunction {
    internal const val NAME: String = "\$__current_user"
    override val signature: FunctionSignature = FunctionSignature(
        name = NAME,
        requiredParameters = emptyList(),
        returnType = StaticType.unionOf(StaticType.STRING, StaticType.NULL)
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue = when (session.currentUser) {
        null -> ExprValue.newNull(IonType.STRING)
        else -> ExprValue.newString(session.currentUser)
    }
}
