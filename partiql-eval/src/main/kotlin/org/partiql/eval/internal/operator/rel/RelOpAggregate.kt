package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.spi.function.Accumulator
import org.partiql.spi.value.Datum
import java.util.TreeMap
import java.util.TreeSet

internal class RelOpAggregate(
    private val input: ExprRelation,
    private val aggregates: List<Aggregate>,
    private val groups: List<ExprValue>,
) : ExprRelation {

    private lateinit var records: Iterator<Row>

    private val aggregationMap = TreeMap<Array<Datum>, List<AccumulatorWrapper>>(DatumArrayComparator)

    /**
     * Wraps an [Accumulator] to help with filtering distinct values.
     *
     * @property seen maintains which values have already been seen. If null, we accumulate all values coming through.
     */
    class AccumulatorWrapper(
        val delegate: Accumulator,
        val args: List<ExprValue>,
        val seen: TreeSet<Array<Datum>>?
    )

    override fun open(env: Environment) {
        input.open(env)
        for (inputRecord in input) {

            // Initialize the AggregationMap
            val evaluatedGroupByKeys = Array(groups.size) { keyIndex ->
                val env = env.push(inputRecord)
                val key = groups[keyIndex].eval(env)
                when (key.isMissing) {
                    true -> Datum.nullValue()
                    false -> key
                }
            }

            val accumulators = aggregationMap.getOrPut(evaluatedGroupByKeys) {
                aggregates.map {
                    AccumulatorWrapper(
                        delegate = it.agg.accumulator,
                        args = it.args,
                        seen = if (it.distinct) TreeSet(DatumArrayComparator) else null
                    )
                }
            }

            // Aggregate Values in Aggregation State
            accumulators.forEachIndexed { index, function ->
                val arguments = Array(function.args.size) {
                    val argument = function.args[it].eval(env.push(inputRecord))
                    // Skip over aggregation if NULL/MISSING
                    if (argument.isNull || argument.isMissing) {
                        return@forEachIndexed
                    }
                    argument
                }
                // Skip over aggregation if DISTINCT and SEEN
                if (function.seen != null && (function.seen.add(arguments).not())) {
                    return@forEachIndexed
                }
                accumulators[index].delegate.next(arguments)
            }

            // TODO env.pop() which happens automatically because the variable is dropped.
        }

        // No Aggregations Created
        if (groups.isEmpty() && aggregationMap.isEmpty()) {
            val record = mutableListOf<Datum>()
            aggregates.forEach { function ->
                val accumulator = function.agg.accumulator
                record.add(accumulator.value())
            }
            records = iterator { yield(Row(record.toTypedArray())) }
            return
        }

        records = iterator {
            aggregationMap.forEach { (keysEvaluated, accumulators) ->
                val recordValues = accumulators.map { acc -> acc.delegate.value() } + keysEvaluated
                yield(Row(recordValues.toTypedArray()))
            }
        }
    }

    override fun hasNext(): Boolean {
        return records.hasNext()
    }

    override fun next(): Row {
        return records.next()
    }

    override fun close() {
        aggregationMap.clear()
        input.close()
    }
}
