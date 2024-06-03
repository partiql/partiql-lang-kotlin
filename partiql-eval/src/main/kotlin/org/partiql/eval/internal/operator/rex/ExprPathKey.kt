package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathKey(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PQLValue {
        val rootEvaluated = root.eval(env).check(PartiQLValueType.STRUCT)
        val keyEvaluated = key.eval(env).check(PartiQLValueType.STRING)
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return PQLValue.nullValue()
        }
        val keyString = keyEvaluated.string
        for (entry in rootEvaluated.fields) {
            if (entry.name == keyString) {
                return entry.value
            }
        }
        throw TypeCheckException()
    }
}
