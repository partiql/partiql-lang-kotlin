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

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprAggregator
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.createUniqueExprValueFilter
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.numberValue
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.div
import org.partiql.lang.util.plus
import java.math.BigDecimal

internal sealed class Accumulator(
    internal open val filter: (ExprValue) -> Boolean
) : ExprAggregator {
    companion object {
        internal fun create(funcName: String, quantifier: PartiqlPhysical.SetQuantifier, valueFactory: ExprValueFactory): Accumulator {
            val filter = when (quantifier) {
                is PartiqlPhysical.SetQuantifier.Distinct -> createUniqueExprValueFilter()
                is PartiqlPhysical.SetQuantifier.All -> { _: ExprValue -> true }
            }
            return when (funcName.trim().toLowerCase()) {
                "min" -> AccumulatorMin(valueFactory, filter)
                "max" -> AccumulatorMax(valueFactory, filter)
                "avg" -> AccumulatorAvg(valueFactory, filter)
                "count" -> AccumulatorCount(valueFactory, filter)
                "sum" -> AccumulatorSum(valueFactory, filter)
                "group_as" -> AccumulatorGroupAs(valueFactory, filter)
                else -> throw IllegalArgumentException("Unsupported aggregation function: $funcName")
            }
        }
    }

    override fun next(value: ExprValue) {
        if (value.isUnknown() || filter.invoke(value).not()) return
        nextValue(value)
    }

    abstract fun nextValue(value: ExprValue)
}

internal class AccumulatorSum(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number? = null

    override fun nextValue(value: ExprValue) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (sum == null) sum = 0L
        this.sum = value.numberValue() + this.sum!!
    }

    override fun compute(): ExprValue {
        return sum?.exprValue(valueFactory) ?: valueFactory.nullValue
    }
}

internal class AccumulatorAvg(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number = 0.0
    var count: Long = 0L

    override fun nextValue(value: ExprValue) {
        checkIsNumberType(funcName = "AVG", value = value)
        this.sum += value.numberValue()
        this.count += 1L
    }

    override fun compute(): ExprValue = when (count) {
        0L -> valueFactory.nullValue
        else -> (sum / bigDecimalOf(count)).exprValue(valueFactory)
    }
}

internal class AccumulatorMax(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var max: ExprValue = valueFactory.nullValue

    override fun nextValue(value: ExprValue) {
        max = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_DESC)(max, value)
    }

    override fun compute(): ExprValue = max
}

internal class AccumulatorMin(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var min: ExprValue = valueFactory.nullValue

    override fun nextValue(value: ExprValue) {
        min = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_ASC)(min, value)
    }

    override fun compute(): ExprValue = min
}

internal class AccumulatorCount(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var count: Long = 0L

    override fun nextValue(value: ExprValue) {
        this.count += 1L
    }

    override fun compute(): ExprValue = count.exprValue(valueFactory)
}

internal class AccumulatorGroupAs(
    internal val valueFactory: ExprValueFactory,
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    val exprValues = mutableListOf<ExprValue>()

    override fun nextValue(value: ExprValue) {
        exprValues.add(value)
    }

    override fun compute(): ExprValue = valueFactory.newBag(exprValues)
}

private fun comparisonAccumulator(comparator: NaturalExprValueComparators): (ExprValue?, ExprValue) -> ExprValue =
    { left, right ->
        when {
            left == null || comparator.compare(left, right) > 0 -> right
            else -> left
        }
    }

internal fun checkIsNumberType(funcName: String, value: ExprValue) {
    if (!value.type.isNumber) {
        errNoContext(
            message = "Aggregate function $funcName expects arguments of NUMBER type but the following value was provided: ${value.ionValue}, with type of ${value.type}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
            internal = false
        )
    }
}

private fun Number.exprValue(valueFactory: ExprValueFactory): ExprValue = when (this) {
    is Int -> valueFactory.newInt(this)
    is Long -> valueFactory.newInt(this)
    is Double -> valueFactory.newFloat(this)
    is BigDecimal -> valueFactory.newDecimal(this)
    else -> errNoContext(
        "Cannot convert number to expression value: $this",
        errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION,
        internal = true
    )
}
