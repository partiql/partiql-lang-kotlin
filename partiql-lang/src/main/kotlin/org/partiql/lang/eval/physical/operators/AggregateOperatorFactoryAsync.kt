/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME
import java.util.TreeMap

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Aggregate] operator.
 *
 * @constructor
 *
 * @param name
 */
public abstract class AggregateOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    public override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.AGGREGATE, name)

    public abstract fun create(
        source: RelationExpressionAsync,
        strategy: PartiqlPhysical.GroupingStrategy,
        keys: List<CompiledGroupKeyAsync>,
        functions: List<CompiledAggregateFunctionAsync>
    ): RelationExpressionAsync
}

public class CompiledGroupKeyAsync(
    val setGroupKeyVal: SetVariableFunc,
    val value: ValueExpressionAsync,
    val variable: PartiqlPhysical.VarDecl
)

public class CompiledAggregateFunctionAsync(
    val name: String,
    val setAggregateVal: SetVariableFunc,
    val value: ValueExpressionAsync,
    val quantifier: PartiqlPhysical.SetQuantifier,
)

internal object AggregateOperatorFactoryDefaultAsync : AggregateOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        source: RelationExpressionAsync,
        strategy: PartiqlPhysical.GroupingStrategy,
        keys: List<CompiledGroupKeyAsync>,
        functions: List<CompiledAggregateFunctionAsync>
    ): RelationExpressionAsync = AggregateOperatorDefaultAsync(source, keys, functions)
}

internal class AggregateOperatorDefaultAsync(
    val source: RelationExpressionAsync,
    val keys: List<CompiledGroupKeyAsync>,
    val functions: List<CompiledAggregateFunctionAsync>
) : RelationExpressionAsync {
    override suspend fun evaluateAsync(state: EvaluatorState): RelationIterator = relation(RelationType.BAG) {
        val aggregationMap = TreeMap<ExprValue, List<Accumulator>>(DEFAULT_COMPARATOR)

        val sourceIter = source.evaluateAsync(state)
        while (sourceIter.nextRow()) {

            // Initialize the AggregationMap
            val evaluatedGroupByKeys =
                keys.map { it.value.invoke(state) }.let { ExprValue.newList(it) }
            val accumulators = aggregationMap.getOrPut(evaluatedGroupByKeys) {
                functions.map { function ->
                    Accumulator.create(function.name, function.quantifier)
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
                val accumulator = Accumulator.create(function.name, function.quantifier)
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
