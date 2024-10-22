package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathKey(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): Datum {
        val rootEvaluated = root.eval(env).check(PartiQLValueType.STRUCT)
        val keyEvaluated = key.eval(env).check(PartiQLValueType.STRING)
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return Datum.nullValue()
        }
        val keyString = keyEvaluated.string
        return rootEvaluated.get(keyString) ?: throw TypeCheckException()
    }
}
