package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Agg
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue
import java.util.TreeMap
import java.util.TreeSet

internal class RelAggregate(
    val input: Operator.Relation,
    val keys: List<Operator.Expr>,
    val functions: List<Operator.Aggregation>
) : Operator.Relation {

    lateinit var records: Iterator<Record>

    @OptIn(PartiQLValueExperimental::class)
    val aggregationMap = TreeMap<List<PartiQLValue>, List<AccumulatorWrapper>>(PartiQLValueListComparator)

    @OptIn(PartiQLValueExperimental::class)
    object PartiQLValueListComparator : Comparator<List<PartiQLValue>> {
        private val delegate = PartiQLValue.comparator(nullsFirst = false)
        override fun compare(o1: List<PartiQLValue>, o2: List<PartiQLValue>): Int {
            if (o1.size < o2.size) {
                return -1
            }
            if (o1.size > o2.size) {
                return 1
            }
            for (index in 0..o2.lastIndex) {
                val element1 = o1[index]
                val element2 = o2[index]
                val compared = delegate.compare(element1, element2)
                if (compared != 0) {
                    return compared
                }
            }
            return 0
        }
    }

    /**
     * Wraps an [Agg.Accumulator] to help with filtering distinct values.
     *
     * @property seen maintains which values have already been seen. If null, we accumulate all values coming through.
     */
    class AccumulatorWrapper @OptIn(PartiQLValueExperimental::class) constructor(
        val delegate: Agg.Accumulator,
        val args: List<Operator.Expr>,
        val seen: TreeSet<List<PartiQLValue>>?
    )

    @OptIn(PartiQLValueExperimental::class)
    override fun open(env: Environment) {
        input.open(env)
        for (inputRecord in input) {
            // Initialize the AggregationMap
            val evaluatedGroupByKeys = keys.map {
                val key = it.eval(env.push(inputRecord))
                when (key.isMissing) {
                    true -> nullValue()
                    false -> key.toPartiQLValue()
                }
            }
            val accumulators = aggregationMap.getOrPut(evaluatedGroupByKeys) {
                functions.map {
                    AccumulatorWrapper(
                        delegate = it.delegate.accumulator(),
                        args = it.args,
                        seen = when (it.setQuantifier) {
                            Operator.Aggregation.SetQuantifier.DISTINCT -> TreeSet(PartiQLValueListComparator)
                            Operator.Aggregation.SetQuantifier.ALL -> null
                        }
                    )
                }
            }

            // Aggregate Values in Aggregation State
            accumulators.forEachIndexed { index, function ->
                // TODO: Add support for aggregating PQLValues directly
                val arguments = function.args.map { it.eval(env.push(inputRecord)) }
                // Skip over aggregation if NULL/MISSING
                if (arguments.any { it.isMissing || it.isNull }) {
                    return@forEachIndexed
                }
                // TODO: Add support for a Datum comparator. Currently, this conversion is inefficient.
                val valuesToCompare = arguments.map { it.toPartiQLValue() }
                // Skip over aggregation if DISTINCT and SEEN
                if (function.seen != null && (function.seen.add(valuesToCompare).not())) {
                    return@forEachIndexed
                }
                accumulators[index].delegate.next(arguments.toTypedArray())
            }
        }

        // No Aggregations Created
        if (keys.isEmpty() && aggregationMap.isEmpty()) {
            val record = mutableListOf<Datum>()
            functions.forEach { function ->
                val accumulator = function.delegate.accumulator()
                record.add(accumulator.value())
            }
            records = iterator { yield(Record.of(*record.toTypedArray())) }
            return
        }

        records = iterator {
            aggregationMap.forEach { (keysEvaluated, accumulators) ->
                val recordValues = accumulators.map { acc -> acc.delegate.value() } + keysEvaluated.map { value -> Datum.of(value) }
                yield(Record.of(*recordValues.toTypedArray()))
            }
        }
    }

    override fun hasNext(): Boolean {
        return records.hasNext()
    }

    override fun next(): Record {
        return records.next()
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun close() {
        aggregationMap.clear()
        input.close()
    }
}
