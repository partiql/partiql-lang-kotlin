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

package org.partiql.eval.functions.aggregate

import org.partiql.errors.ErrorCode
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprAggregator
import org.partiql.lang.eval.PartiQLValue
import org.partiql.lang.eval.PartiQLValueType
import org.partiql.lang.eval.NaturalPartiQLValueComparators
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.createUniquePartiQLValueFilter
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.numberValue
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.div
import org.partiql.lang.util.exprValue
import org.partiql.lang.util.plus
import org.partiql.value.PartiQLValue

internal sealed class Accumulator(
    internal open val filter: (PartiQLValue) -> Boolean
) : ExprAggregator {
    companion object {
        internal fun create(funcName: String, quantifier: PartiqlPhysical.SetQuantifier): Accumulator {
            val filter = when (quantifier) {
                is PartiqlPhysical.SetQuantifier.Distinct -> createUniquePartiQLValueFilter()
                is PartiqlPhysical.SetQuantifier.All -> { _: PartiQLValue -> true }
            }
            return when (funcName.trim().lowercase()) {
                "min" -> AccumulatorMin(filter)
                "max" -> AccumulatorMax(filter)
                "avg" -> AccumulatorAvg(filter)
                "count" -> AccumulatorCount(filter)
                "sum" -> AccumulatorSum(filter)
                "group_as" -> AccumulatorGroupAs(filter)
                "every" -> AccumulatorEvery(filter)
                "any" -> AccumulatorAnySome(filter)
                "some" -> AccumulatorAnySome(filter)
                else -> throw IllegalArgumentException("Unsupported aggregation function: $funcName")
            }
        }
    }

    override fun next(value: PartiQLValue) {
        if (value.isUnknown() || filter.invoke(value).not()) return
        nextValue(value)
    }

    abstract fun nextValue(value: PartiQLValue)
}

internal class AccumulatorSum(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number? = null

    override fun nextValue(value: PartiQLValue) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (sum == null) sum = 0L
        this.sum = value.numberValue() + this.sum!!
    }

    override fun compute(): PartiQLValue {
        return sum?.exprValue() ?: PartiQLValue.nullValue
    }
}

internal class AccumulatorAvg(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number = 0.0
    var count: Long = 0L

    override fun nextValue(value: PartiQLValue) {
        checkIsNumberType(funcName = "AVG", value = value)
        this.sum += value.numberValue()
        this.count += 1L
    }

    override fun compute(): PartiQLValue = when (count) {
        0L -> PartiQLValue.nullValue
        else -> (sum / bigDecimalOf(count)).exprValue()
    }
}

internal class AccumulatorMax(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    var max: PartiQLValue = PartiQLValue.nullValue

    override fun nextValue(value: PartiQLValue) {
        max = comparisonAccumulator(NaturalPartiQLValueComparators.NULLS_LAST_DESC)(max, value)
    }

    override fun compute(): PartiQLValue = max
}

internal class AccumulatorMin(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    var min: PartiQLValue = PartiQLValue.nullValue

    override fun nextValue(value: PartiQLValue) {
        min = comparisonAccumulator(NaturalPartiQLValueComparators.NULLS_LAST_ASC)(min, value)
    }

    override fun compute(): PartiQLValue = min
}

internal class AccumulatorCount(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    var count: Long = 0L

    override fun nextValue(value: PartiQLValue) {
        this.count += 1L
    }

    override fun compute(): PartiQLValue = count.exprValue()
}

internal class AccumulatorEvery(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    private var res: PartiQLValue? = null
    override fun nextValue(value: PartiQLValue) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { PartiQLValue.newBoolean(it.booleanValue() && value.booleanValue()) } ?: value
    }

    override fun compute(): PartiQLValue = res ?: PartiQLValue.nullValue
}

internal class AccumulatorAnySome(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    private var res: PartiQLValue? = null
    override fun nextValue(value: PartiQLValue) {
        checkIsBooleanType("ANY/SOME", value)
        res = res?.let { PartiQLValue.newBoolean(it.booleanValue() || value.booleanValue()) } ?: value
    }

    override fun compute(): PartiQLValue = res ?: PartiQLValue.nullValue
}

internal class AccumulatorGroupAs(
    internal override val filter: (PartiQLValue) -> Boolean
) : Accumulator(filter = filter) {

    val exprValues = mutableListOf<PartiQLValue>()

    override fun nextValue(value: PartiQLValue) {
        exprValues.add(value)
    }

    override fun compute(): PartiQLValue = PartiQLValue.newBag(exprValues)
}

private fun comparisonAccumulator(comparator: NaturalPartiQLValueComparators): (PartiQLValue?, PartiQLValue) -> PartiQLValue =
    { left, right ->
        when {
            left == null || comparator.compare(left, right) > 0 -> right
            else -> left
        }
    }

internal fun checkIsNumberType(funcName: String, value: PartiQLValue) {
    if (!value.type.isNumber) {
        errNoContext(
            message = "Aggregate function $funcName expects arguments of NUMBER type but the following value was provided: $value, with type of ${value.type}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
            internal = false
        )
    }
}

internal fun checkIsBooleanType(funcName: String, value: PartiQLValue) {
    if (value.type != PartiQLValueType.BOOL) {
        errNoContext(
            message = "Aggregate function $funcName expects arguments of BOOL type but the following value was provided: $value, with type of ${value.type}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
            internal = false
        )
    }
}
