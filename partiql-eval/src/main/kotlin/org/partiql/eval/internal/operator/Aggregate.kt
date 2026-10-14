package org.partiql.eval.internal.operator

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.spi.function.Accumulator
import org.partiql.spi.function.Agg
import org.partiql.spi.value.Datum
import java.util.TreeSet

/**
 * Simple data class to hold a compile aggregation call.
 */
internal class Aggregate(
    val agg: Agg,
    val args: List<ExprValue>,
    val distinct: Boolean
) {

    /**
     * Creates a fresh aggregation state (a new [Accumulator] and, if [distinct], a new set of seen values).
     */
    fun newState(): State = State()

    /**
     * The running state of a single aggregation. This is shared by group aggregation and aggregate window functions so
     * that both have identical NULL/MISSING and DISTINCT semantics.
     */
    inner class State {

        private val accumulator: Accumulator = agg.accumulator

        /**
         * Maintains which values have already been seen. If null, we accumulate all values coming through.
         */
        private val seen: TreeSet<Array<Datum>>? = if (distinct) TreeSet(DatumArrayComparator) else null

        /**
         * Evaluates the arguments against [env] (which must have the input row pushed) and feeds them to the
         * accumulator. Inputs having any NULL/MISSING argument are skipped (SQL-99 6.16 General Rules on
         * <set function specification>), as are already-seen inputs when [distinct].
         */
        fun accumulate(env: Environment) {
            val arguments = Array(args.size) {
                val argument = args[it].eval(env)
                // Skip over aggregation if NULL/MISSING
                if (argument.isNull || argument.isMissing) {
                    return
                }
                argument
            }
            // Skip over aggregation if DISTINCT and SEEN
            if (seen != null && seen.add(arguments).not()) {
                return
            }
            accumulator.next(arguments)
        }

        /**
         * @return the current value of the aggregation.
         */
        fun value(): Datum = accumulator.value()
    }
}
