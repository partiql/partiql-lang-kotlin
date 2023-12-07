package org.partiql.eval.functions.scalar

import com.amazon.ion.IonType
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.ConnectorSession
import org.partiql.lang.eval.PartiQLFunction
import org.partiql.lang.eval.PartiQLValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

internal val SYSTEM_BUILTINS_SQL = listOf<PartiQLFunction>(
    PartiQLFunctionCurrentUser
)

internal object PartiQLFunctionCurrentUser : PartiQLFunction {
    internal const val FUNCTION_NAME: String = "\$__current_user"

    override val signature: FunctionSignature = FunctionSignature(
        name = FUNCTION_NAME,
        requiredParameters = emptyList(),
        returnType = StaticType.unionOf(StaticType.STRING, StaticType.NULL)
    )

    override fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue {
        return when (val user = session.context[ConnectorSession.Constants.CURRENT_USER_KEY]) {
            is String -> PartiQLValue.newString(user)
            null -> PartiQLValue.newNull(IonType.STRING)
            else -> err(
                message = "CURRENT_USER must be either a STRING or NULL.",
                errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE,
                errorContext = errorContextFrom(emptyMetaContainer()),
                internal = false
            )
        }
    }
}
