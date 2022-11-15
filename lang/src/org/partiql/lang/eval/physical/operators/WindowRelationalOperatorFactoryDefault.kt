package org.partiql.lang.eval.physical.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.window.ExperimentalWindowFunc
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
@ExperimentalWindowFunc
internal object WindowRelationalOperatorFactoryDefault : WindowRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>,
        windowSortSpecList: List<CompiledSortKey>,
        compiledWindowFunctions: List<CompiledWindowFunction>
    ): RelationExpression = WindowOperatorDefault(source, windowPartitionList, windowSortSpecList, compiledWindowFunctions)
}

@ExperimentalWindowFunc
internal class WindowOperatorDefault(
    private val source: RelationExpression,
    private val windowPartitionList: List<ValueExpression>,
    private val windowSortSpecList: List<CompiledSortKey>,
    private val compiledWindowFunctions: List<CompiledWindowFunction>
) : RelationExpression {
    override fun evaluate(state: EvaluatorState): RelationIterator {
        // the following corresponding to materialization process
        val sourceIter = source.evaluate(state)
        val registers = sequence {
            while (sourceIter.nextRow()) {
                yield(state.registers.clone())
            }
        }

        val partitionSortSpec = windowPartitionList.map {
            CompiledSortKey(NaturalExprValueComparators.NULLS_FIRST_ASC, it)
        }

        val sortKeys = partitionSortSpec + windowSortSpecList

        val sortedRegisters = registers.sortedWith(getSortingComparator(sortKeys, state))

        // create the partition here
        var partition = mutableListOf<List<Array<ExprValue>>>()

        // entire partition
        if (windowPartitionList.isEmpty()) {
            partition.add(sortedRegisters.toList())
        }
        // need to be partitioned
        else {
            val iter = sortedRegisters.iterator()
            var rowInPartition = mutableListOf<Array<ExprValue>>()
            var previousPartition: ExprValue? = null
            while (iter.hasNext()) {
                val currentRow = iter.next()
                state.load(currentRow)
                val currentPartition = state.valueFactory.newSexp(
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
