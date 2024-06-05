package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

internal class ExprCollection(
    private val values: List<Operator.Expr>,
    private val type: StaticType
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        return when (type) {
            is BagType -> Datum.bagValue(values.map { it.eval(env) })
            is SexpType -> Datum.sexpValue(values.map { it.eval(env) })
            is ListType -> Datum.listValue(values.map { it.eval(env) })
            else -> error("Unsupported type for collection $type")
        }
    }
}
