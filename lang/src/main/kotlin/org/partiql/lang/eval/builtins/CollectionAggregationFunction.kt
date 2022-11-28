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

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.physical.operators.Accumulator
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import kotlin.reflect.full.primaryConstructor

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
internal sealed class CollectionAggregationFunction(private val valueFactory: ExprValueFactory) : ExprFunction {

    internal abstract val aggregationName: String

    enum class Parameters(val index: Int, val type: StaticType) {
        QUANTIFIER(0, StaticType.STRING),
        ARGUMENT(1, AnyOfType(setOf(StaticType.LIST, StaticType.BAG, StaticType.STRUCT, StaticType.SEXP)))
    }

    companion object {
        internal const val collectionAggregationPrefix = "coll_"
        internal fun createAll(valueFactory: ExprValueFactory): List<CollectionAggregationFunction> =
            CollectionAggregationFunction::class.sealedSubclasses.map { subClass ->
                subClass.primaryConstructor?.call(valueFactory)!!
            }
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val inputSequence = required[Parameters.ARGUMENT.index]
        val quantifier = getQuantifier(required[Parameters.QUANTIFIER.index].stringValue())
        val accumulator = Accumulator.create(aggregationName, quantifier, valueFactory)
        inputSequence.asSequence().forEach { exprValue -> accumulator.next(exprValue) }
        return accumulator.compute()
    }

    private fun getQuantifier(quantifierText: String) = when (quantifierText.toLowerCase().trim()) {
        "all" -> PartiqlPhysical.SetQuantifier.All()
        "distinct" -> PartiqlPhysical.SetQuantifier.Distinct()
        else -> throw IllegalArgumentException("Unrecognized set quantifier: $quantifierText")
    }

    internal fun getFunctionSignature(aggregationName: String) = FunctionSignature(
        name = "$collectionAggregationPrefix$aggregationName",
        requiredParameters = listOf(Parameters.QUANTIFIER.type, Parameters.ARGUMENT.type),
        returnType = StaticType.NUMERIC
    )
}

internal class CollectionMaxFunction(valueFactory: ExprValueFactory) : CollectionAggregationFunction(valueFactory) {
    override val aggregationName: String = "max"
    override val signature = getFunctionSignature(this.aggregationName)
}

internal class CollectionMinFunction(valueFactory: ExprValueFactory) : CollectionAggregationFunction(valueFactory) {
    override val aggregationName: String = "min"
    override val signature = getFunctionSignature(this.aggregationName)
}

internal class CollectionAvgFunction(valueFactory: ExprValueFactory) : CollectionAggregationFunction(valueFactory) {
    override val aggregationName: String = "avg"
    override val signature = getFunctionSignature(this.aggregationName)
}

internal class CollectionSumFunction(valueFactory: ExprValueFactory) : CollectionAggregationFunction(valueFactory) {
    override val aggregationName: String = "sum"
    override val signature = getFunctionSignature(this.aggregationName)
}
internal class CollectionCountFunction(valueFactory: ExprValueFactory) : CollectionAggregationFunction(valueFactory) {
    override val aggregationName: String = "count"
    override val signature = getFunctionSignature(this.aggregationName)
}
