package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.toSetVariableFunc
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation

class CompiledSortKey(val comparator: NaturalExprValueComparators, val value: ValueExpression)

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
class sortBasedWindowOperator(name: String) : WindowRelationalOperatorFactory(name) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>?,
        windowSortSpecList: List<CompiledSortKey>?,
        windowExpression: PartiqlPhysical.WindowExpression,
        windowFunctionParameter: List<ValueExpression>
    ) = RelationExpression { state ->

        // the following corresponding to materialization process
        val source = source.evaluate(state)
        val registers = mutableListOf<Array<ExprValue>>()
        while (source.nextRow()) {
            registers.add(state.registers.clone())
        }
        // if partition and order by are both null, we do not sort
        // this logic will not be called as this point since lag/lead forcefully require ORDER BY

        val partitionSortSpec = windowPartitionList?.map {
            CompiledSortKey(NaturalExprValueComparators.NULLS_FIRST_ASC, it)
        } ?: emptyList<CompiledSortKey>()

        val sortKeys = partitionSortSpec + (windowSortSpecList ?: emptyList<CompiledSortKey>())

        val sortedRegisters = registers.sortedWith(getSortingComparator(sortKeys, state))

        // create the partition here TODO refactor the partition creation logic
        var partition = mutableListOf<List<Array<ExprValue>>>()

        // entire partition
        if (windowPartitionList == null) {
            partition.add(sortedRegisters)
        }
        // need to be partitioned
        else {
            val iter = sortedRegisters.iterator()
            var rowInPartition = mutableListOf<Array<ExprValue>>()
            var previousPartition: ExprValue? = null
            while (iter.hasNext()) {
                val currentRow = iter.next()
                transferState(state, currentRow)
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

        relation(RelationType.LIST) {
            partition.forEach { rowsInPartition ->
                // calculate window function result
                if (windowExpression.funcName.text.toLowerCase() == "lag") {
                    rowsInPartition.forEachIndexed { index, row ->
                        // TODO need additional check logic to valiadate window function parameters
                        val (target, offset, default) = when (windowFunctionParameter.size) {
                            1 -> listOf(windowFunctionParameter[0], null, null)

                            2 -> listOf(windowFunctionParameter[0], windowFunctionParameter[1], null)

                            3 -> listOf(windowFunctionParameter[0], windowFunctionParameter[1], null)

                            else -> error("Wrong number of Parameter for LAG Function")
                        }

                        // reset index for parameter evaluation
                        transferState(state, row)
                        val offsetValue = offset?.invoke(state)?.numberValue()?.toLong() ?: 1
                        val defaultValue = default?.invoke(state) ?: state.valueFactory.nullValue
                        val targetIndex = index - offsetValue
                        // if targetRow is within partition
                        if (targetIndex >= 0 && targetIndex <= rowsInPartition.size - 1) {
                            // TODO need to check if index is larger than MAX INT, but this may causes overflow already
                            val targetRow = rowsInPartition[targetIndex.toInt()]
                            transferState(state, targetRow)
                            val res = target!!.invoke(state)
                            transferState(state, row)
                            windowExpression.decl.toSetVariableFunc()(state, res)
                        } else {
                            transferState(state, row)
                            windowExpression.decl.toSetVariableFunc()(state, defaultValue)
                        }
                        yield()
                    }
                } else {
                    rowsInPartition.forEachIndexed { index, row ->
                        // TODO need additional check logic to valiadate window function parameters
                        val (target, offset, default) = when (windowFunctionParameter.size) {
                            1 -> listOf(windowFunctionParameter[0], null, null)

                            2 -> listOf(windowFunctionParameter[0], windowFunctionParameter[1], null)

                            3 -> listOf(windowFunctionParameter[0], windowFunctionParameter[1], null)

                            else -> error("Wrong number of Parameter for Lead Function")
                        }

                        // reset index for parameter evaluation
                        transferState(state, row)
                        val offsetValue = offset?.invoke(state)?.numberValue()?.toLong() ?: 1
                        val defaultValue = default?.invoke(state) ?: state.valueFactory.nullValue
                        val targetIndex = index + offsetValue
                        // if targetRow is within partition
                        if (targetIndex >= 0 && targetIndex <= rowsInPartition.size - 1) {
                            // TODO need to check if index is larger than MAX INT, but this may causes overflow already
                            val targetRow = rowsInPartition[targetIndex.toInt()]
                            transferState(state, targetRow)
                            val res = target!!.invoke(state)
                            transferState(state, row)
                            windowExpression.decl.toSetVariableFunc()(state, res)
                        } else {
                            transferState(state, row)
                            windowExpression.decl.toSetVariableFunc()(state, defaultValue)
                        }
                        yield()
                    }
                }
            }
        }
    }
}
