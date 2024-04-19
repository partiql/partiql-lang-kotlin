package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.nullValue

internal class ExprPathKey(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PartiQLValue {
        val rootEvaluated = root.eval(env).check<StructValue<PartiQLValue>>()
        val keyEvaluated = key.eval(env).check<StringValue>()
        val keyString = keyEvaluated.value ?: error("String value was null")
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return nullValue()
        }
        return rootEvaluated[keyString] ?: throw TypeCheckException()
    }
}
