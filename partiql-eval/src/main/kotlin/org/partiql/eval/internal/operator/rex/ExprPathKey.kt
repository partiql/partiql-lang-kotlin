package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathKey(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(): Datum {
        val rootEvaluated = root.eval().check(PartiQLValueType.STRUCT)
        val keyEvaluated = key.eval().check(PartiQLValueType.STRING)
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return Datum.nullValue()
        }
        val keyString = keyEvaluated.string
        return rootEvaluated.get(keyString) ?: throw TypeCheckException()
    }
}
