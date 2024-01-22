package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.util.PartiQLValueComparator
import org.partiql.plan.Rel
import org.partiql.value.PartiQLValueExperimental

internal class RelSort(
    val input: Operator.Relation,
    val specs: List<Pair<Operator.Expr, Rel.Op.Sort.Order>>

) : Operator.Relation {
    private var records: MutableList<Record> = mutableListOf()
    private var init: Boolean = false

    private val nullsFirstComparator = PartiQLValueComparator(nullOrder = PartiQLValueComparator.NullOrder.FIRST)
    private val nullsLastComparator = PartiQLValueComparator(nullOrder = PartiQLValueComparator.NullOrder.LAST)

    override fun open() {
        input.open()
        init = false
        records = mutableListOf()
    }

    @OptIn(PartiQLValueExperimental::class)
    val comparator = object : Comparator<Record> {
        override fun compare(l: Record, r: Record): Int {
            specs.forEach { spec ->
                val lVal = spec.first.eval(l)
                val rVal = spec.first.eval(r)

                // DESC_NULLS_FIRST(l, r) == ASC_NULLS_LAST(r, l)
                // DESC_NULLS_LAST(l, r) == ASC_NULLS_FIRST(r, l)
                val cmpResult = when (spec.second) {
                    Rel.Op.Sort.Order.ASC_NULLS_FIRST -> nullsFirstComparator.compare(lVal, rVal)
                    Rel.Op.Sort.Order.ASC_NULLS_LAST -> nullsLastComparator.compare(lVal, rVal)
                    Rel.Op.Sort.Order.DESC_NULLS_FIRST -> nullsLastComparator.compare(rVal, lVal)
                    Rel.Op.Sort.Order.DESC_NULLS_LAST -> nullsFirstComparator.compare(rVal, lVal)
                }
                if (cmpResult != 0) {
                    return cmpResult
                }
            }
            return 0 // Equal
        }
    }

    override fun next(): Record? {
        if (!init) {
            while (true) {
                val row = input.next() ?: break
                records.add(row)
            }
            records.sortWith(comparator)
        }
        return when (records.isEmpty()) {
            true -> null
            else -> records.removeAt(0)
        }
    }

    override fun close() {
        input.close()
    }
}
