/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.createUniqueExprValueFilter
import org.partiql.lang.eval.physical.operators.Accumulator
import org.partiql.lang.eval.physical.operators.AccumulatorAvg
import org.partiql.lang.eval.physical.operators.AccumulatorCount
import org.partiql.lang.eval.physical.operators.AccumulatorMax
import org.partiql.lang.eval.physical.operators.AccumulatorMin
import org.partiql.lang.eval.physical.operators.AccumulatorSum
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.unionOf

/**
 * TODO replace this internal value once we have function libraries
 */
internal val SCALAR_BUILTINS_COLL_AGG = listOf(
    ExprFunctionCollMax,
    ExprFunctionCollMin,
    ExprFunctionCollAvg,
    ExprFunctionCollSum,
    ExprFunctionCollCount
)

/**
 * This class represents an aggregation function call (such as AVG, MAX, MIN, etc) -- but is meant to be operated outside
 * of the relational algebra implementation of `aggregate`. In other words, the [CollectionAggregationFunction] allows
 * users to call aggregation functions such as "AVG" on collections of scalars. While a user may use the function with the
 * "Direct Usage" below, this function is also used within PartiQL to convert aggregate function calls that are outside
 * of the scope of the relational algebra operator of aggregations. AKA -- we use this when the aggregation function calls
 * are made outside of the projection clause, the HAVING clause, and ORDER BY clause.
 *
 * Direct Usage: coll_{AGGREGATE}('all', [0, 1, 2, 3])
 * where ${AGGREGATE} can be replaced with MAX, MIN, AVG, COUNT, and SUM
 *
 * Example (Direct) Usage:
 * ```
 * SELECT a AS inputA, COLL_AVG(a) AS averagedA
 * FROM << {'a': [0, 1]}, {'a': [10, 11]} >>
 * WHERE COLL_AVG(a) > 0.5
 * ```
 *
 * Example (Indirect) Usage:
 * ```
 * SELECT a
 * FROM << {'a': [0, 1]}, {'a': [10, 11]} >>
 * WHERE AVG(a) > 0.5
 * ```
 *
 * The above indirect example shows how this is leveraged. The WHERE clause does not allow aggregation functions to be passed to the
 * aggregate operator, so we internally convert the AVG to a [CollectionAggregationFunction] (which is just an expression
 * function call).
 */
internal sealed class CollectionAggregationFunction(
    val name: String,
    val accumulator: ((ExprValue) -> Boolean) -> Accumulator,
) : ExprFunction {

    companion object {
        const val PREFIX = "coll_"
    }

    private val collection = unionOf(StaticType.LIST, StaticType.BAG, StaticType.STRUCT, StaticType.SEXP)

    override val signature: FunctionSignature = FunctionSignature(
        name = "$PREFIX$name",
        requiredParameters = listOf(StaticType.STRING, collection),
        returnType = StaticType.NUMERIC
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val filter = required[0].asQuantifierFilter()
        val collection = required[1].asSequence()
        // instantiate a fresh accumulator for each invocation as this is a "tuple-level" function
        val acc = accumulator(filter)
        collection.forEach { v -> acc.next(v) }
        return acc.compute()
    }

    private fun ExprValue.asQuantifierFilter() = when (stringValue().toLowerCase().trim()) {
        "all" -> { _: ExprValue -> true }
        "distinct" -> createUniqueExprValueFilter()
        else -> throw IllegalArgumentException("Unrecognized set quantifier: $this")
    }
}

internal object ExprFunctionCollMax : CollectionAggregationFunction(
    name = "max",
    accumulator = ::AccumulatorMax,
)

internal object ExprFunctionCollMin : CollectionAggregationFunction(
    name = "min",
    accumulator = ::AccumulatorMin
)

internal object ExprFunctionCollAvg : CollectionAggregationFunction(
    name = "avg",
    accumulator = ::AccumulatorAvg
)

internal object ExprFunctionCollSum : CollectionAggregationFunction(
    name = "sum",
    accumulator = ::AccumulatorSum
)

internal object ExprFunctionCollCount : CollectionAggregationFunction(
    name = "count",
    accumulator = ::AccumulatorCount
)
