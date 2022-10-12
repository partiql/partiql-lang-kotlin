/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.physical.operators

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME
import org.partiql.lang.util.interruptibleFold

internal object SortOperatorFactoryDefault : SortOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        sortKeys: List<CompiledSortKey>,
        sourceRelation: RelationExpression
    ): RelationExpression = SortOperatorDefault(sortKeys, sourceRelation)
}

internal class SortOperatorDefault(private val sortKeys: List<CompiledSortKey>, private val sourceRelation: RelationExpression) : RelationExpression {
    override fun evaluate(state: EvaluatorState): RelationIterator {
        val source = sourceRelation.evaluate(state)
        return relation(RelationType.LIST) {
            val rows = mutableListOf<Array<ExprValue>>()
            val comparator = getSortingComparator(sortKeys, state)

            // Consume Input
            while (source.nextRow()) {
                rows.add(state.registers.clone())
            }

            // Perform Sort
            val sortedRows = rows.sortedWith(comparator)

            // Yield Sorted Rows
            val iterator = sortedRows.iterator()
            while (iterator.hasNext()) {
                state.load(iterator.next())
                yield()
            }
        }
    }
}

/**
 * Returns a [Comparator] that compares arrays of registers by using un-evaluated sort keys. It does this by modifying
 * the [state] to allow evaluation of the [sortKeys]
 */
private fun getSortingComparator(sortKeys: List<CompiledSortKey>, state: EvaluatorState): Comparator<Array<ExprValue>> {
    val initial: Comparator<Array<ExprValue>>? = null
    return sortKeys.interruptibleFold(initial) { intermediate, sortKey ->
        if (intermediate == null) {
            return@interruptibleFold compareBy<Array<ExprValue>, ExprValue>(sortKey.comparator) { row ->
                state.load(row)
                sortKey.value(state)
            }
        }
        return@interruptibleFold intermediate.thenBy(sortKey.comparator) { row ->
            state.load(row)
            sortKey.value(state)
        }
    } ?: err(
        "Order BY comparator cannot be null",
        ErrorCode.EVALUATOR_ORDER_BY_NULL_COMPARATOR,
        null,
        internal = true
    )
}
