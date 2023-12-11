package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.missingValue

internal class ExprPathKey(
    val root: Operator.Expr,
    val key: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        val rootEvaluated = root.eval(record).check<StructValue<PartiQLValue>>()
        val keyEvaluated = key.eval(record).check<StringValue>()
        val keyString = keyEvaluated.value ?: error("String value was null")
        return rootEvaluated[keyString] ?: missingValue()
    }
}
