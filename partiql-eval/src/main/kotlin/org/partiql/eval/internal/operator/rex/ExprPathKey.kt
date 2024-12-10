package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprPathKey(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val rootEvaluated = root.eval(env).check(PType.struct())
        val keyEvaluated = key.eval(env).check(PType.string())
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return Datum.nullValue()
        }
        val keyString = keyEvaluated.string
        return rootEvaluated.get(keyString) ?: throw TypeCheckException()
    }
}
