package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import org.partiql.spi.value.Datum
import java.util.Collections

internal class RelOpSort(
    private val input: Relation,
    private val collations: List<Collation>,
) : Relation {
    private var records: Iterator<Record> = Collections.emptyIterator()
    private var init: Boolean = false

    private val nullsFirstComparator = Datum.comparator(true)
    private val nullsLastComparator = Datum.comparator(false)

    private lateinit var env: Environment

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
        init = false
        records = Collections.emptyIterator()
    }

    private val comparator = object : Comparator<Record> {
        override fun compare(l: Record, r: Record): Int {
            collations.forEach { spec ->
                // TODO: Write comparator for PQLValue
                val lVal = spec.expr.eval(env.push(l))
                val rVal = spec.expr.eval(env.push(r))

                // DESC_NULLS_FIRST(l, r) == ASC_NULLS_LAST(r, l)
                // DESC_NULLS_LAST(l, r) == ASC_NULLS_FIRST(r, l)
                val cmpResult = when {
                    !spec.desc && !spec.last -> nullsFirstComparator.compare(lVal, rVal)
                    !spec.desc && spec.last -> nullsLastComparator.compare(lVal, rVal)
                    spec.desc && !spec.last -> nullsLastComparator.compare(rVal, lVal)
                    spec.desc && spec.last -> nullsFirstComparator.compare(rVal, lVal)
                    else -> 0 // unreachable
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

    /**
     * DO NOT USE FINAL.
     *
     * @property expr   The expression to sort by..
     * @property desc   True iff DESC sort, otherwise ASC.
     * @property last   True iff NULLS LAST sort, otherwise NULLS FIRST.
     */
    class Collation(
        @JvmField var expr: Expression,
        @JvmField var desc: Boolean,
        @JvmField var last: Boolean,
    )
}
