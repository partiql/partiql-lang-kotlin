package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.nullValue
import org.partiql.value.structValue

internal class RelJoinLeft(
    override val lhs: Operator.Relation,
    override val rhs: Operator.Relation,
    override val condition: Operator.Expr
) : RelJoin() {

    override fun getOutputRecord(result: Boolean, lhs: Record, rhs: Record): Record {
        if (result.not()) {
            rhs.padNull()
        }
        return lhs + rhs
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun Record.padNull() {
        this.values.indices.forEach { index ->
            this.values[index] = values[index].padNull()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.padNull(): PartiQLValue {
        return when (this) {
            is StructValue<*> -> {
                val newFields = this.fields?.map { it.first to nullValue() }
                structValue(newFields)
            }
            else -> nullValue()
        }
    }
}
