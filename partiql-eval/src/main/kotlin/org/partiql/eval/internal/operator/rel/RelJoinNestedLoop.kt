package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.nullValue
import org.partiql.value.structValue
import java.util.Stack

internal abstract class RelJoinNestedLoop : Operator.Relation {

    abstract val lhs: Operator.Relation
    abstract val rhs: Operator.Relation
    abstract val condition: Operator.Expr
    abstract val scopes: Stack<Record>

    private var lhsRecord: Record? = null

    override fun open() {
        lhs.open()
        lhsRecord = lhs.next()
        scopes.push(lhsRecord)
        rhs.open()
        scopes.pop()
    }

    abstract fun join(condition: Boolean, lhs: Record, rhs: Record): Record?

    @OptIn(PartiQLValueExperimental::class)
    override fun next(): Record? {
        var rhsRecord = rhs.next()
        var toReturn: Record? = null
        do {
            // Acquire LHS and RHS Records
            if (rhsRecord == null) {
                rhs.close()
                lhsRecord = lhs.next() ?: return null
                scopes.push(lhsRecord)
                rhs.open()
                rhsRecord = rhs.next()
                scopes.pop()
            }
            // Return Joined Record
            if (rhsRecord != null && lhsRecord != null) {
                val input = lhsRecord!! + rhsRecord
                val result = condition.eval(input)
                toReturn = join(result.isTrue(), lhsRecord!!, rhsRecord)
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
                val newFields = this.entries.map { it.first to nullValue() }
                structValue(newFields)
            }
            else -> nullValue()
        }
    }
}
