package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.structValue

internal class ExprStruct(val fields: List<Field>) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        val fields = fields.map {
            val key = it.key.eval(record) as? StringValue ?: error("Expected struct key to be a STRING.")
            val value = it.key.eval(record)
            key.value!! to value
        }
        return structValue(fields.asSequence())
    }

    internal class Field(val key: Operator.Expr, val value: Operator.Expr)
}
