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

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
abstract class WindowRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        impl: PartiqlPhysical.Impl,

        source: RelationExpression,

        windowPartitionList: List<ValueExpression>,

        windowSortSpecList: List<CompiledSortKey>,

        windowExpression: PartiqlPhysical.WindowExpression,

        windowFunctionParameter: List<ValueExpression>,

    ): RelationExpression
}

class SortBasedWindowOperator(name: String) : WindowRelationalOperatorFactory(name) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>,
        windowSortSpecList: List<CompiledSortKey>,
        windowExpression: PartiqlPhysical.WindowExpression,
        windowFunctionParameter: List<ValueExpression>
    ) = RelationExpression { state ->

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
                val currentPartition = state.valueFactory.newSexp(
                    windowPartitionList.map {
                        it.invoke(state)
                    }
                )

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

        // We would need to model this better. This PR was for demonstrate the window operator.
        // See the next PR for window function modeling.
        if (windowExpression.funcName.text.toLowerCase() == "lag") {
            LagFunction(windowExpression, partition, windowFunctionParameter, state).eval()
        } else {
            LeadFunction(windowExpression, partition, windowFunctionParameter, state).eval()
        }
    }
}

// TODO : The next PR will focus on modeling the window function.
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
                    state.load(row)
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
                    val targetIndex = index + offsetValue
                    // if targetRow is within partition
                    if (targetIndex <= rowsInPartition.lastIndex) {
                        val targetRow = rowsInPartition[targetIndex.toInt()]
                        state.load(targetRow)
                        val res = target!!.invoke(state)
                        state.load(row)
                        windowExpression.decl.toSetVariableFunc()(state, res)
                    } else {
                        state.load(row)
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
                    state.load(row)
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
                        val targetRow = rowsInPartition[targetIndex.toInt()]
                        state.load(targetRow)
                        val res = target!!.invoke(state)
                        state.load(row)
                        windowExpression.decl.toSetVariableFunc()(state, res)
                    } else {
                        state.load(row)
                        windowExpression.decl.toSetVariableFunc()(state, defaultValue)
                    }
                    yield()
                }
            }
        }
    }
}
