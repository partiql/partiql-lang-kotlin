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

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

internal object SortOperatorFactoryDefaultAsync : SortOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        sortKeys: List<CompiledSortKeyAsync>,
        sourceRelation: RelationExpressionAsync
    ): RelationExpressionAsync = SortOperatorDefaultAsync(sortKeys, sourceRelation)
}

internal class SortOperatorDefaultAsync(private val sortKeys: List<CompiledSortKeyAsync>, private val sourceRelation: RelationExpressionAsync) : RelationExpressionAsync {
    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
        val source = sourceRelation.evaluate(state)
        return relation(RelationType.LIST) {
            val rows = mutableListOf<Array<ExprValue>>()

            // Consume Input
            while (source.nextRow()) {
                rows.add(state.registers.clone())
            }

            val rowWithValues = rows.map { row ->
                state.load(row)
                row to sortKeys.map { sk ->
                    sk.value(state)
                }
            }.toMutableList()
            val comparator = getSortingComparator(sortKeys.map { it.comparator })

            // Perform Sort
            val sortedRows = rowWithValues.sortedWith(comparator)

            // Yield Sorted Rows
            val iterator = sortedRows.iterator()
            while (iterator.hasNext()) {
                state.load(iterator.next().first)
                yield()
            }
        }
    }
}

/**
 * Returns a [Comparator] that compares arrays of registers by using un-evaluated sort keys. It does this by modifying
 * the [EvaluatorState] to allow evaluation of the [sortKeys].
 */
internal fun getSortingComparator(sortKeys: List<NaturalExprValueComparators>): Comparator<Pair<Array<ExprValue>, List<ExprValue>>> {
    return object : Comparator<Pair<Array<ExprValue>, List<ExprValue>>> {
        override fun compare(
            l: Pair<Array<ExprValue>, List<ExprValue>>,
            r: Pair<Array<ExprValue>, List<ExprValue>>
        ): Int {
            val valsToCompare = l.second.zip(r.second)
            sortKeys.zip(valsToCompare).map {
                val comp = it.first
                val cmpResult = comp.compare(it.second.first, it.second.second)
                if (cmpResult != 0) {
                    return cmpResult
                }
            }
            return 0
        }
    }
}
