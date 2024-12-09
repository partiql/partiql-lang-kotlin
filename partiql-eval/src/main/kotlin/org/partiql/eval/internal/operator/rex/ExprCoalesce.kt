package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental

internal class ExprCoalesce(
    private val args: Array<ExprValue>
) : ExprValue {

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        for (arg in args) {
            val result = arg.eval(env)
            if (!result.isNull && !result.isMissing) {
                return result
            }
        }
        return Datum.nullValue()
    }
}
