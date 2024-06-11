package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental

internal class ExprCollection(
    private val values: List<Operator.Expr>,
    private val type: PType
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        return when (type.kind) {
            PType.Kind.BAG -> Datum.bagValue(values.map { it.eval(env) })
            PType.Kind.SEXP -> Datum.sexpValue(values.map { it.eval(env) })
            PType.Kind.LIST -> Datum.listValue(values.map { it.eval(env) })
            else -> error("Unsupported type for collection $type")
        }
    }
}
