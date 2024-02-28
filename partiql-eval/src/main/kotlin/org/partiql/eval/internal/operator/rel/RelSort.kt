package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Rel
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.util.Collections

@OptIn(PartiQLValueExperimental::class)
internal class RelSort(
    private val input: Operator.Relation,
    private val specs: List<Pair<Operator.Expr, Rel.Op.Sort.Order>>

) : Operator.Relation {
    private var records: Iterator<Record> = Collections.emptyIterator()
    private var init: Boolean = false

    private val nullsFirstComparator = PartiQLValue.comparator(nullsFirst = true)
    private val nullsLastComparator = PartiQLValue.comparator(nullsFirst = false)

    private lateinit var env: Environment

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
        init = false
        records = Collections.emptyIterator()
    }

    private val comparator = object : Comparator<Record> {
        override fun compare(l: Record, r: Record): Int {
            specs.forEach { spec ->
                val lVal = spec.first.eval(env.push(l))
                val rVal = spec.first.eval(env.push(r))

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

    override fun hasNext(): Boolean {
        if (!init) {
            val sortedRecords = mutableListOf<Record>()
            for (row in input) {
                sortedRecords.add(row)
            }
            sortedRecords.sortWith(comparator)
            records = sortedRecords.iterator()
            init = true
        }
        return records.hasNext()
    }

    override fun next(): Record {
        return records.next()
    }

    override fun close() {
        init = false
        input.close()
    }
}
