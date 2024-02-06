package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.listValue
import org.partiql.value.sexpValue

internal class ExprCollection(
    private val values: List<Operator.Expr>,
    private val type: StaticType
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        return when (type) {
            is BagType -> bagValue(values.map { it.eval(record) })
            is SexpType -> sexpValue(values.map { it.eval(record) })
            is ListType -> listValue(values.map { it.eval(record) })
            else -> error("Unsupported type for collection $type")
        }
    }
}
