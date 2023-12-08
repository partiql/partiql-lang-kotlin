package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.structValue

@OptIn(PartiQLValueExperimental::class)
internal class ExprPivot(
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(record: Record): PartiQLValue {
        input.open()
        val fields = mutableListOf<Pair<String, PartiQLValue>>()
        while (true) {
            val row = input.next() ?: break
            val k = key.eval(row) as? StringValue ?: error("PIVOT key must be a StringValue")
            val v = value.eval(row)
            fields.add(k.value!! to v)
        }
        input.close()
        return structValue(fields)
    }
}
