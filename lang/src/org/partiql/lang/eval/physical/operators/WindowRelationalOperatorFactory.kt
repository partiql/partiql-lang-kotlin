package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.toSetVariableFunc
import org.partiql.lang.eval.relation.RelationIterator
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

        // We would need to model this better
        // ideally, we would have a factory that binds window function name and parameter to an implementation
        // ideally, window function are processed per row, i.e., we have processing one window function call per row
        // We could benefit from have a window function interface which serves as a top-level abstraction
        // "partition based window function" and "frame based window function" will inherit from "window function"
        // and concrete window function implementations are inherited from the above two.
        // This is why, simplification such as abstract Lag/Lead into a LagLeadCommon has not been done yet.
        if (windowExpression.funcName.text.toLowerCase() == "lag") {
            LagFunction(windowExpression, partition, windowFunctionParameter, state).eval()
        } else {
            LeadFunction(windowExpression, partition, windowFunctionParameter, state).eval()
        }
    }
}

internal class LeadFunction(val windowExpression: PartiqlPhysical.WindowExpression, val partition: MutableList<List<Array<ExprValue>>>, val arguments: List<ValueExpression>, val state: EvaluatorState) {

    fun eval(): RelationIterator {
        val (target, offset, default) = when (arguments.size) {
            1 -> listOf(arguments[0], null, null)

            2 -> listOf(arguments[0], arguments[1], null)

            3 -> listOf(arguments[0], arguments[1], arguments[2])

            else -> error("Wrong number of Parameter for Lead Function")
        }
        return relation(RelationType.BAG) {

            partition.forEach { rowsInPartition ->
                rowsInPartition.forEachIndexed { index, row ->
                    // reset index for parameter evaluation
                    transferState(state, row)
                    val offsetValue = offset?.let {
                        val numberValue = it.invoke(state).numberValue().toLong()
                        // taking one step back here, do we even want to support non-constant value?
                        if (numberValue >= 0) {
                            numberValue
                        } else {
                            error("offset need to be non-negative integer")
                        }
                    } ?: 1L // default offset is one
                    // We leave the checking mechanism for type mismatch out for now.
                    val defaultValue = default?.invoke(state) ?: state.valueFactory.nullValue
                    val targetIndex = index + offsetValue.toLong()
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

internal class LagFunction(val windowExpression: PartiqlPhysical.WindowExpression, val partition: MutableList<List<Array<ExprValue>>>, val arguments: List<ValueExpression>, val state: EvaluatorState) {

    fun eval(): RelationIterator {
        val (target, offset, default) = when (arguments.size) {
            1 -> listOf(arguments[0], null, null)

            2 -> listOf(arguments[0], arguments[1], null)

            3 -> listOf(arguments[0], arguments[1], arguments[2])

            else -> error("Wrong number of Parameter for Lag Function")
        }
        return relation(RelationType.BAG) {

            partition.forEach { rowsInPartition ->
                rowsInPartition.forEachIndexed { index, row ->
                    // reset index for parameter evaluation
                    transferState(state, row)
                    val offsetValue = offset?.let {
                        val numberValue = it.invoke(state).numberValue().toLong()
                        // taking one step back here, do we even want to support non-constant value?
                        if (numberValue >= 0) {
                            numberValue
                        } else {
                            error("offset need to be non-negative integer")
                        }
                    } ?: 1L // default offset is one
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
            }
        }
    }
}
