package org.partiql.lang.eval.physical.operators

import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * This is an experimental implementation of the window operator
 *
 * The general concept here is to sort the input relation, first by partition keys (if not null) then by sort keys (if not null).
 * After sorting, we do a sequence scan to create partition and materialize all the element in the same partition
 *
 */
@ExperimentalWindowFunctions
internal object WindowRelationalOperatorFactoryDefaultAsync : WindowRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        source: RelationExpressionAsync,
        windowPartitionList: List<ValueExpressionAsync>,
        windowSortSpecList: List<CompiledSortKeyAsync>,
        compiledWindowFunctions: List<CompiledWindowFunctionAsync>
    ): RelationExpressionAsync = WindowOperatorDefaultAsync(source, windowPartitionList, windowSortSpecList, compiledWindowFunctions)
}

@ExperimentalWindowFunctions
internal class WindowOperatorDefaultAsync(
    private val source: RelationExpressionAsync,
    private val windowPartitionList: List<ValueExpressionAsync>,
    private val windowSortSpecList: List<CompiledSortKeyAsync>,
    private val compiledWindowFunctions: List<CompiledWindowFunctionAsync>
) : RelationExpressionAsync {
    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
        // the following corresponding to materialization process
        val sourceIter = source.evaluate(state)
        val registers = sequence {
            while (sourceIter.nextRow()) {
                yield(state.registers.clone())
            }
        }

        val partitionSortSpec = windowPartitionList.map {
            CompiledSortKeyAsync(NaturalExprValueComparators.NULLS_FIRST_ASC, it)
        }

        val sortKeys = partitionSortSpec + windowSortSpecList

        val newRegisters = registers.toList().map { row ->
            state.load(row)
            row to sortKeys.map { sk ->
                sk.value(state)
            }
        }.toMutableList()

        val sortedRegisters = newRegisters.sortedWith(getSortingComparator(sortKeys.map { it.comparator })).map { it.first }

        // create the partition here
        val partition = mutableListOf<List<Array<ExprValue>>>()

        // entire partition
        if (windowPartitionList.isEmpty()) {
            partition.add(sortedRegisters.toList())
        }
        // need to be partitioned
        else {
            val iter = sortedRegisters.iterator()
            val rowInPartition = mutableListOf<Array<ExprValue>>()
            var previousPartition: ExprValue? = null
            while (iter.hasNext()) {
                val currentRow = iter.next()
                state.load(currentRow)
                val currentPartition = ExprValue.newSexp(
                    windowPartitionList.map {
                        it.invoke(state)
                    }
                )
                // for the first time,
                if (previousPartition == null) {
                    rowInPartition.add(currentRow)
                    previousPartition = currentPartition
                } else if (previousPartition.exprEquals(currentPartition)) {
                    rowInPartition.add(currentRow)
                } else {
                    partition.add(rowInPartition.toList())
                    rowInPartition.clear()
                    previousPartition = currentPartition
                    rowInPartition.add(currentRow)
                }
            }
            // finish up
            partition.add(rowInPartition.toList())
            rowInPartition.clear()
        }

        return relation(RelationType.BAG) {
            partition.forEach { rowsInPartition ->
                compiledWindowFunctions.forEach {
                    val windowFunc = it.func
                    // set the window function partition to the current partition
                    windowFunc.reset(rowsInPartition)
                }

                rowsInPartition.forEach {
                    // process current row
                    compiledWindowFunctions.forEach { compiledWindowFunction ->
                        compiledWindowFunction.func.processRow(state, compiledWindowFunction.parameters, compiledWindowFunction.windowVarDecl)
                    }

                    // yield the result
                    yield()
                }
            }
        }
    }
}
