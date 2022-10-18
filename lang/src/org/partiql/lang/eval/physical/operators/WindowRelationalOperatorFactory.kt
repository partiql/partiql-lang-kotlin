package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.physical.window.WindowFunction
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation

abstract class WindowRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        impl: PartiqlPhysical.Impl,

        source: RelationExpression,

        windowPartitionList: List<ValueExpression>?,

        windowSortSpecList: List<CompiledSortKey>?,

        windowExpression: PartiqlPhysical.WindowExpression,

        windowFunctionParameter: List<ValueExpression>,

        windowFunctionMap: Map<String, WindowFunction>

    ): RelationExpression
}

/**
 * This is an experimental implementation of the window operator
 * Many concepts are missing from this implementation as the first step is to implementation partition based function `LAG` and `LEAD`.
 *
 * The general concept here is to sort the input relation, first by partition keys (if not null) then by sort keys ( if not null).
 * After sorting, we can do a sequence scan to create partition and materialize all the element in the same partition
 * After partition is materialized, `LAG` and `LEAD` function can use index to access the target row, if the target row is with in the partition.
 *
 */
class SortBasedWindowOperator(name: String) : WindowRelationalOperatorFactory(name) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>?,
        windowSortSpecList: List<CompiledSortKey>?,
        windowExpression: PartiqlPhysical.WindowExpression,
        windowFunctionParameter: List<ValueExpression>,
        windowFunctionMap: Map<String, WindowFunction>
    ) = RelationExpression { state ->

        // the following corresponding to materialization process
        val source = source.evaluate(state)
        val registers = sequence {
            while (source.nextRow()) {
                yield(state.registers.clone())
            }
        }

        val partitionSortSpec = windowPartitionList?.map {
            CompiledSortKey(NaturalExprValueComparators.NULLS_FIRST_ASC, it)
        } ?: emptyList()

        val sortKeys = partitionSortSpec + (windowSortSpecList ?: emptyList())

        val sortedRegisters = registers.sortedWith(getSortingComparator(sortKeys, state))

        // create the partition here
        var partition = mutableListOf<List<Array<ExprValue>>>()

        // entire partition
        if (windowPartitionList == null) {
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

        val windowFunction = windowFunctionMap[windowExpression.funcName.text]
            ?: error("window function not yet implemented")

        relation(RelationType.BAG) {
            partition.forEach { rowsInPartition ->

                // set the window function partition to the current partition
                windowFunction.reset(rowsInPartition)

                rowsInPartition.forEach { row ->
                    // reset state
                    state.load(row)

                    // process current row
                    windowFunction.processRow(state, windowFunctionParameter, windowExpression.decl)

                    // yield the result
                    yield()
                }
            }
        }
    }
}
