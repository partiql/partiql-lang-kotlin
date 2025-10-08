package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.spi.value.Datum
import java.util.Collections

internal class RelOpSort(
    private val input: ExprRelation,
    private val collations: List<Collation>,
) : ExprRelation {
    private var records: Iterator<Row> = Collections.emptyIterator()
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

    private val comparator = object : Comparator<Row> {
        override fun compare(l: Row, r: Row): Int {
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
            val sortedRows = mutableListOf<Row>()
            for (row in input) {
                sortedRows.add(row)
            }
            sortedRows.sortWith(comparator)
            records = sortedRows.iterator()
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
}
