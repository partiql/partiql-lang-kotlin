package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue

internal class ExprCollection(
    private val values: List<Operator.Expr>
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        return bagValue(values.map { it.eval(record) })
    }
}
