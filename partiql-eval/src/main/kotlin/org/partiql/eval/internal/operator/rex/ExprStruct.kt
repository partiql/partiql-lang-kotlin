package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.check
import org.partiql.value.structValue

internal class ExprStruct(val fields: List<Field>) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        val fields = fields.mapNotNull {
            val key = it.key.eval(record).check<StringValue>()
            when (val value = it.value.eval(record)) {
                is MissingValue -> null
                else -> key.value!! to value
            }
        }
        return structValue(fields)
    }

    internal class Field(val key: Operator.Expr, val value: Operator.Expr)
}
