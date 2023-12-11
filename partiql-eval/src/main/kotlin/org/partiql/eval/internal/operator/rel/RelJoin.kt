package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal abstract class RelJoin : Operator.Relation {

    abstract val lhs: Operator.Relation
    abstract val rhs: Operator.Relation
    abstract val condition: Operator.Expr

    private val lhsStored = mutableListOf<Record>()
    private val rhsStored = mutableListOf<Record>()
    private lateinit var lhsIterator: Iterator<Record>
    private lateinit var rhsIterator: Iterator<Record>

    override fun open() {
        lhs.open()
        rhs.open()

        var x = lhs.next()
        while (x != null) {
            lhsStored.add(x)
            x = lhs.next()
        }

        var rhsIter = rhs.next()
        while (rhsIter != null) {
            rhsStored.add(rhsIter)
            rhsIter = rhs.next()
        }
        lhsIterator = lhsStored.iterator()
        rhsIterator = rhsStored.iterator()
    }

    @OptIn(PartiQLValueExperimental::class)
    abstract fun getOutputRecord(result: Boolean, lhs: Record, rhs: Record): Record?

    @OptIn(PartiQLValueExperimental::class)
    override fun next(): Record? {
        lhsIterator.forEach { lhsRecord ->
            rhsIterator.forEach { rhsRecord ->
                val input = lhsRecord + rhsRecord
                val result = condition.eval(input)
                getOutputRecord(result.isTrue(), lhsRecord, rhsRecord)?.let { return it }
            }
        }
        return null
    }

    override fun close() {
        lhs.close()
        lhsStored.clear()
        rhs.close()
        rhsStored.clear()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.isTrue(): Boolean {
        return this is BoolValue && this.value == true
    }
}
