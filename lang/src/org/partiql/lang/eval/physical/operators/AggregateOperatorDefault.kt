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

import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import java.util.TreeMap

/**
 * The [AggregateOperatorFactoryDefault] performs a simple form of aggregation by computing the group keys and t
 */
internal class AggregateOperatorDefault : AggregateOperator {
    override fun create(
        source: RelationExpression,
        keys: List<CompiledGroupKey>,
        functions: List<CompiledAggregateFunction>
    ): RelationExpression =
        RelationExpression { state ->
            relation(RelationType.BAG) {

                val aggregationMap = TreeMap<ExprValue, List<Accumulator>>(DEFAULT_COMPARATOR)

                val sourceIter = source.evaluate(state)
                while (sourceIter.nextRow()) {

                    // Initialize the AggregationMap
                    val evaluatedGroupByKeys =
                        keys.map { it.value.invoke(state) }.let { state.valueFactory.newList(it) }
                    val accumulators = aggregationMap.getOrPut(evaluatedGroupByKeys) {
                        functions.map { function ->
                            Accumulator.create(function.name, function.quantifier, state.valueFactory)
                        }
                    }

                    // Aggregate Values in Aggregation State
                    functions.forEachIndexed { index, function ->
                        val valueToAggregate = function.value(state)
                        accumulators[index].next(valueToAggregate)
                    }
                }

                // No Aggregations Created
                if (keys.isEmpty() && aggregationMap.isEmpty()) {
                    functions.forEach { function ->
                        val accumulator = Accumulator.create(function.name, function.quantifier, state.valueFactory)
                        function.setAggregateVal(state, accumulator.compute())
                    }
                    yield()
                    return@relation
                }

                // Place Aggregated Values into Result State
                aggregationMap.forEach { (exprList, accumulators) ->
                    exprList.forEachIndexed { index, exprValue -> keys[index].setGroupKeyVal(state, exprValue) }
                    accumulators.forEachIndexed { index, acc -> functions[index].setAggregateVal(state, acc.compute()) }
                    yield()
                }
            }
        }
}
