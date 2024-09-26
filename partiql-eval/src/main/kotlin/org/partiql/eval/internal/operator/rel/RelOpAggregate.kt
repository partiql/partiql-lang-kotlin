package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.function.Aggregation
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.util.TreeMap
import java.util.TreeSet

internal class RelOpAggregate(
    val input: Operator.Relation,
    private val keys: List<Operator.Expr>,
    private val functions: List<Operator.Aggregation>
) : Operator.Relation {

    private lateinit var records: Iterator<Record>

    private val aggregationMap = TreeMap<Array<Datum>, List<AccumulatorWrapper>>(DatumArrayComparator)

    /**
     * Wraps an [Aggregation.Accumulator] to help with filtering distinct values.
     *
     * @property seen maintains which values have already been seen. If null, we accumulate all values coming through.
     */
    class AccumulatorWrapper(
        val delegate: Aggregation.Accumulator,
        val args: List<Operator.Expr>,
        val seen: TreeSet<Array<Datum>>?
    )

    override fun open(env: Environment) {
        input.open(env)
        for (inputRecord in input) {
            // Initialize the AggregationMap
            val evaluatedGroupByKeys = Array(keys.size) { keyIndex ->
                val key = keys[keyIndex].eval(env.push(inputRecord))
                when (key.isMissing) {
                    true -> Datum.nullValue()
                    false -> key
                }
            }

            // TODO IT DOES NOT MATTER NOW, BUT SqlCompiler SHOULD HANDLE GET THE ARGUMENT TYPES FOR .getAccumulator
            val args: Array<PType> = emptyArray()

            val accumulators = aggregationMap.getOrPut(evaluatedGroupByKeys) {
                functions.map {
                    AccumulatorWrapper(
                        delegate = it.delegate.getAccumulator(args),
                        args = it.args,
                        seen = when (it.setQuantifier) {
                            Operator.Aggregation.SetQuantifier.DISTINCT -> TreeSet(DatumArrayComparator)
                            Operator.Aggregation.SetQuantifier.ALL -> null
                        }
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
        }

        // No Aggregations Created
        if (keys.isEmpty() && aggregationMap.isEmpty()) {
            val record = mutableListOf<Datum>()
            functions.forEach { function ->
                val accumulator = function.delegate.getAccumulator(args = emptyArray())
                record.add(accumulator.value())
            }
            records = iterator { yield(Record.of(*record.toTypedArray())) }
            return
        }

        records = iterator {
            aggregationMap.forEach { (keysEvaluated, accumulators) ->
                val recordValues = accumulators.map { acc -> acc.delegate.value() } + keysEvaluated
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

    override fun close() {
        aggregationMap.clear()
        input.close()
    }
}
