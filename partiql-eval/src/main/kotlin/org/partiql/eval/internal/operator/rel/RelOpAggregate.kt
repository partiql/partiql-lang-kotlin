package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.spi.value.Datum
import java.util.TreeMap

internal class RelOpAggregate(
    private val input: ExprRelation,
    private val aggregates: List<Aggregate>,
    private val groups: List<ExprValue>,
) : ExprRelation {

    private lateinit var records: Iterator<Row>

    private val aggregationMap = TreeMap<Array<Datum>, List<Aggregate.State>>(DatumArrayComparator)

    override fun open(env: Environment) {
        input.open(env)
        for (inputRecord in input) {
            checkInterrupted()

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
                aggregates.map { it.newState() }
            }

            // Aggregate Values in Aggregation State
            val inputEnv = env.push(inputRecord)
            accumulators.forEach { it.accumulate(inputEnv) }

            // TODO env.pop() which happens automatically because the variable is dropped.
        }

        // No Aggregations Created
        if (groups.isEmpty() && aggregationMap.isEmpty()) {
            val record = Array<Datum?>(aggregates.size) {
                val function = aggregates[it]
                val accumulator = function.agg.accumulator
                accumulator.value()
            }
            records = iterator { yield(Row(record)) }
            return
        }

        records = iterator {
            aggregationMap.forEach { (keysEvaluated, accumulators) ->
                val accumulatorValues = Array(accumulators.size) { i -> accumulators[i].value() }
                val recordValues = accumulatorValues + keysEvaluated
                yield(Row(recordValues))
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
