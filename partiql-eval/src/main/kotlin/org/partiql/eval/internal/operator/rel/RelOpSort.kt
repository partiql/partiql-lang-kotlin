package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import java.util.Collections

internal class RelOpSort(
    private val env: Environment,
    private val input: Operator.Relation,
    private val collations: List<Collation>,
) : Operator.Relation {
    private var records: Iterator<Row> = Collections.emptyIterator()
    private var init: Boolean = false

    private val nullsFirstComparator = Datum.comparator(true)
    private val nullsLastComparator = Datum.comparator(false)

    override fun open() {
        input.open()
        init = false
        records = Collections.emptyIterator()
    }

    private val comparator = object : Comparator<Row> {
        override fun compare(l: Row, r: Row): Int {
            collations.forEach { spec ->
                // TODO: Write comparator for PQLValue
                val lVal = env.scope(l) { spec.expr.eval() }
                val rVal = env.scope(r) { spec.expr.eval() }

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
            val sortedRecords = mutableListOf<Row>()
            for (row in input) {
                sortedRecords.add(row)
            }
            sortedRecords.sortWith(comparator)
            records = sortedRecords.iterator()
            init = true
        }
        return records.hasNext()
    }

    override fun next(): Row {
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
        @JvmField var expr: Operator.Expr,
        @JvmField var desc: Boolean,
        @JvmField var last: Boolean,
    )
}
