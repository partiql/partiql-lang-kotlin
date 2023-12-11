package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal abstract class RelJoin : Operator.Relation {

    abstract val lhs: Operator.Relation
    abstract val rhs: Operator.Relation

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

    override fun next(): Record? {
        TODO("Not yet implemented")
    }

    override fun close() {
        lhs.close()
        lhsStored.clear()
        rhs.close()
        rhsStored.clear()
    }
}