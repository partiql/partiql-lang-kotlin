package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.nullValue
import org.partiql.value.structValue

internal abstract class RelJoinNestedLoop : Operator.Relation {

    abstract val lhs: Operator.Relation
    abstract val rhs: Operator.Relation
    abstract val condition: Operator.Expr

    private var rhsRecord: Record? = null

    override fun open() {
        lhs.open()
        rhs.open()
        rhsRecord = rhs.next()
    }

    abstract fun join(condition: Boolean, lhs: Record, rhs: Record): Record?

    @OptIn(PartiQLValueExperimental::class)
    override fun next(): Record? {
        var lhsRecord = lhs.next()
        var toReturn: Record? = null
        do {
            // Acquire LHS and RHS Records
            if (lhsRecord == null) {
                lhs.close()
                rhsRecord = rhs.next() ?: return null
                lhs.open()
                lhsRecord = lhs.next()
            }
            // Return Joined Record
            if (lhsRecord != null && rhsRecord != null) {
                val input = lhsRecord + rhsRecord!!
                val result = condition.eval(input)
                toReturn = join(result.isTrue(), lhsRecord, rhsRecord!!)
            }
        }
        while (toReturn == null)
        return toReturn
    }

    override fun close() {
        lhs.close()
        rhs.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.isTrue(): Boolean {
        return this is BoolValue && this.value == true
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun Record.padNull() {
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
