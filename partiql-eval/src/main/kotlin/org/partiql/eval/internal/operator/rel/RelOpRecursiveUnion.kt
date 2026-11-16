package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.spi.value.Datum
import java.util.TreeSet

/**
 * The working table of a recursive WITH list element; it holds the rows produced by the previous iteration of the
 * [RelOpRecursiveUnion], and it is read by [org.partiql.eval.internal.operator.rex.ExprWorkingTable].
 */
internal class WorkingTable {
    var rows: List<Datum> = emptyList()
}

/**
 * Evaluates the body of a recursive WITH list element, `seed UNION [ALL] recursive`.
 *
 * SQL:2023 Part 2, Subclause 7.17, General Rule 2.c.ii, defines the result as a fixpoint: starting from an empty
 * table T, repeatedly replace T with `seed UNION [ALL] recursive(T)` until the cardinality of T does not grow.
 *
 * When [incremental], this computes the same result more efficiently: the seed is evaluated once and becomes the
 * working table; the recursive operand is then evaluated against the working table, and the produced rows (excluding
 * any already produced rows if [distinct]) become the next working table; this repeats until an iteration produces no
 * rows. This is equivalent because the recursion is linear and, for UNION ALL, the recursive operand distributes over
 * the union of its input rows. That does not hold if the recursive operand of a UNION ALL eliminates duplicates
 * (e.g. SELECT DISTINCT), in which case the fixpoint is computed as specified.
 *
 * Each iteration is materialized before it is used, so the working table is never replaced while the recursive
 * operand is being evaluated. Note that, as per SQL:2023, this does not terminate when the recursion has no fixpoint.
 *
 * @property seed the non-recursive operand.
 * @property recursive the recursive operand, which reads [workingTable].
 * @property workingTable the working table read by [recursive].
 * @property distinct true for UNION DISTINCT, false for UNION ALL.
 * @property incremental true to evaluate incrementally (see above).
 */
internal class RelOpRecursiveUnion(
    private val seed: ExprRelation,
    private val recursive: ExprRelation,
    private val workingTable: WorkingTable,
    private val distinct: Boolean,
    private val incremental: Boolean,
) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)

    private lateinit var env: Environment

    /**
     * The rows produced by the current iteration.
     */
    private var produced: List<Row> = emptyList()

    private var rows: Iterator<Row> = emptyList<Row>().iterator()

    override fun openPeeking(env: Environment) {
        this.env = env
        seen.clear()
        workingTable.rows = emptyList()
        if (incremental) {
            produced = materialize(seed)
            rows = produced.iterator()
        } else {
            produced = emptyList()
            rows = fixpoint().iterator()
        }
    }

    override fun peek(): Row? {
        while (true) {
            if (rows.hasNext()) {
                return rows.next()
            }
            if (produced.isEmpty()) {
                return null
            }
            checkInterrupted()
            workingTable.rows = produced.map { it.values[0] }
            produced = materialize(recursive)
            rows = produced.iterator()
        }
    }

    override fun closePeeking() {
        seen.clear()
        workingTable.rows = emptyList()
        produced = emptyList()
        rows = emptyList<Row>().iterator()
    }

    /**
     * Computes the fixpoint as specified by SQL:2023 Part 2, Subclause 7.17, General Rule 2.c.ii.
     */
    private fun fixpoint(): List<Row> {
        val seedRows = materialize(seed)
        var table = emptyList<Row>()
        var result = seedRows + evaluate(table)
        while (result.size > table.size) {
            checkInterrupted()
            table = result
            result = seedRows + evaluate(table)
        }
        return result
    }

    private fun evaluate(table: List<Row>): List<Row> {
        workingTable.rows = table.map { it.values[0] }
        return materialize(recursive)
    }

    private fun materialize(rel: ExprRelation): List<Row> {
        val result = mutableListOf<Row>()
        rel.open(env)
        try {
            for (row in rel) {
                val values = row.values.copyOf()
                if (distinct) {
                    val key = row.values.copyOf()
                    key.coerceMissing()
                    if (!seen.add(key)) {
                        continue
                    }
                }
                result.add(Row(values))
            }
        } finally {
            rel.close()
        }
        return result
    }
}
