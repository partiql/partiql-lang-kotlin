package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
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
    class AccumulatorWrapper @OptIn(PartiQLValueExperimental::class, FnExperimental::class) constructor(
        val delegate: Agg.Accumulator,
        val args: List<Operator.Expr>,
        val seen: TreeSet<List<PartiQLValue>>?
    )

    @OptIn(PartiQLValueExperimental::class, FnExperimental::class)
    override fun open() {
        input.open()
        while (input.hasNext()) {
            val inputRecord = input.next()
            // Initialize the AggregationMap
            val evaluatedGroupByKeys = keys.map {
                val key = it.eval(inputRecord)
                when (key.type == PartiQLValueType.MISSING) {
                    true -> nullValue()
                    false -> key
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
                val valueToAggregate = function.args.map { it.eval(inputRecord) }
                // Skip over aggregation if NULL/MISSING
                if (valueToAggregate.any { it.type == PartiQLValueType.MISSING || it.isNull }) {
                    return@forEachIndexed
                }
                // Skip over aggregation if DISTINCT and SEEN
                if (function.seen != null && (function.seen.add(valueToAggregate).not())) {
                    return@forEachIndexed
                }
                accumulators[index].delegate.next(valueToAggregate.toTypedArray())
            }
        }

        // No Aggregations Created
        if (keys.isEmpty() && aggregationMap.isEmpty()) {
            val record = mutableListOf<PartiQLValue>()
            functions.forEach { function ->
                val accumulator = function.delegate.accumulator()
                record.add(accumulator.value())
            }
            records = iterator { yield(Record.of(*record.toTypedArray())) }
            return
        }

        records = iterator {
            aggregationMap.forEach { (keysEvaluated, accumulators) ->
                val recordValues = accumulators.map { acc -> acc.delegate.value() } + keysEvaluated.map { value -> value }
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
