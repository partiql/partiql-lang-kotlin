package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum

/**
 * Implementation for variable lookup; walks up environments if necessary, otherwise lookup using tuple offset.
 */
internal class ExprVar(
    private var depth: Int,
    private var offset: Int,
) : ExprValue {

    override fun eval(env: Environment): Datum = env.get(depth, offset)
}
