package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.listValue

internal class ExprCollection(
    private val values: List<Operator.Expr>,
    private val type: PartiQLType
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): PartiQLValue {
        return when (type) {
            is BagType -> bagValue(values.map { it.eval(env) })
            is ArrayType -> listValue(values.map { it.eval(env) })
            else -> error("Unsupported type for collection $type")
        }
    }
}
